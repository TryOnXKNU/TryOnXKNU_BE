
from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.middleware.cors import CORSMiddleware
import os
import io
from PIL import Image
import numpy as np
import json

app = FastAPI(title="Validate Service")

# Allow all origins for local development (restrict in production)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/")
async def root():
    return {"status": "ok", "service": "validate"}


@app.post("/validate")
async def validate(image: UploadFile = File(...)):
    """
    간단한 이미지 검증 엔드포인트(복원용)
    - multipart/form-data 로 `image` 파일을 받음
    - 실제 모델이 없다면 파일 크기 기반의 간단한 heuristic 반환
    - 반환 예시: {"isClothing": True, "confidence": 0.99}
    """
    try:
        contents = await image.read()
        size = len(contents)

        # Try using ONNX model if available
        model_path = os.path.join(os.path.dirname(__file__), "model", "model.onnx")
        metadata_path = os.path.join(os.path.dirname(__file__), "model", "metadata.json")
        # Default threshold is on P(clothing)
        decision_threshold = float(os.getenv("VALIDATE_DECISION_THRESHOLD", "0.36"))
        class_to_idx = None
        if os.path.exists(metadata_path):
            try:
                with open(metadata_path, 'r') as mf:
                    meta = json.load(mf)
                    class_to_idx = meta.get('class_to_idx')
            except Exception:
                class_to_idx = None

        if os.path.exists(model_path):
            try:
                import onnxruntime as ort

                img = Image.open(io.BytesIO(contents)).convert("RGB")
                # preprocess
                img = img.resize((224, 224))
                arr = np.array(img).astype(np.float32) / 255.0
                # normalize (ImageNet)
                mean = np.array([0.485, 0.456, 0.406], dtype=np.float32)
                std = np.array([0.229, 0.224, 0.225], dtype=np.float32)
                arr = (arr - mean) / std
                # HWC -> NCHW
                arr = np.transpose(arr, (2, 0, 1))[None, :]
                # Ensure float32 and C-contiguous for ONNXRuntime
                arr = np.ascontiguousarray(arr, dtype=np.float32)

                sess = ort.InferenceSession(model_path, providers=["CPUExecutionProvider"])
                input_name = sess.get_inputs()[0].name
                out = sess.run(None, {input_name: arr})
                # output named 'logit' per export
                logit = float(np.array(out[0]).ravel()[0])
                prob = 1.0 / (1.0 + float(np.exp(-logit)))
                # Determine P(clothing) from model's output probability.
                # If metadata maps 'clothing' to index 1, prob==P(clothing).
                # If 'clothing' maps to index 0, prob==P(not_clothing) and we invert.
                try:
                    if class_to_idx is not None and 'clothing' in class_to_idx:
                        clothing_idx = int(class_to_idx.get('clothing', 0))
                        if clothing_idx == 1:
                            prob_clothing = prob
                        else:
                            prob_clothing = 1.0 - prob
                    else:
                        prob_clothing = prob
                except Exception:
                    prob_clothing = prob

                is_clothing = bool(prob_clothing > decision_threshold)
                return {"isClothing": is_clothing, "confidence": float(prob_clothing)}
            except Exception:
                # fall back to heuristic below
                pass

        # Fallback heuristic: very small files are not clothing
        if size <= 0:
            return {"isClothing": False, "confidence": 0.0}

        confidence = min(0.999, max(0.05, size / (1024 * 200)))
        is_clothing = confidence > decision_threshold

        return {"isClothing": is_clothing, "confidence": float(confidence)}

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
