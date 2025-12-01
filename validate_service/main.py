# from fastapi import FastAPI, UploadFile, File, HTTPException
# from fastapi.middleware.cors import CORSMiddleware
#
# app = FastAPI(title="Validate Service")
#
# # Allow all origins for local development (restrict in production)
# app.add_middleware(
#     CORSMiddleware,
#     allow_origins=["*"],
#     allow_credentials=True,
#     allow_methods=["*"],
#     allow_headers=["*"],
# )
#
#
# @app.get("/")
# async def root():
#     return {"status": "ok", "service": "validate"}
#
#
# @app.post("/validate")
# async def validate(image: UploadFile = File(...)):
#     """
#     간단한 이미지 검증 엔드포인트(복원용)
#     - multipart/form-data 로 `image` 파일을 받음
#     - 실제 모델이 없다면 파일 크기 기반의 간단한 heuristic 반환
#     - 반환 예시: {"isClothing": True, "confidence": 0.99}
#     """
#     try:
#         contents = await image.read()
#         size = len(contents)
#
#         # 간단한 heuristic: 아주 작은 파일은 의상이 아님
#         if size <= 0:
#             return {"isClothing": False, "confidence": 0.0}
#
#         # 큰 파일일수록 신뢰도 높게 (임시 로직)
#         confidence = min(0.999, max(0.05, size / (1024 * 200)))
#         is_clothing = confidence > 0.18
#
#         return {"isClothing": is_clothing, "confidence": float(confidence)}
#
#     except Exception as e:
#         raise HTTPException(status_code=500, detail=str(e))
from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from io import BytesIO
from PIL import Image
import torch
import clip
import numpy as np

app = FastAPI(title="Validate Service")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

device = "cuda" if torch.cuda.is_available() else "cpu"
model, preprocess = clip.load("ViT-L/14", device=device)

# --- 의류 프롬프트 ---
CLOTHING_PROMPTS = [
    "a t-shirt", "a graphic t-shirt", "a printed t-shirt",
    "a short sleeve shirt", "a long sleeve shirt", "a sweater",
    "a hoodie", "a sweatshirt", "a blouse",
    "a jacket", "a coat",
    "short pants", "long pants", "jeans", "slacks",
    "a skirt", "a dress", "a long dress", "a short dress",
    "a clothing item", "an apparel product",
    "a photo of clothes", "a product photo of clothing",
    "상의", "긴팔티", "반팔티", "긴바지", "반바지", "원피스"
]

# --- 비의류 프롬프트 ---
NOT_CLOTHING_PROMPTS = [
    "a face","a human face","a portrait",
    "a cartoon character","a memoji",
    "a diagram","a flowchart","a UI mockup",
    "a landscape","a food photo",
    "an object","an illustration"
]

CLOTHING_TOKENS = clip.tokenize(CLOTHING_PROMPTS).to(device)
NOT_TOKENS = clip.tokenize(NOT_CLOTHING_PROMPTS).to(device)

def classify_image(image: Image.Image, threshold=0.40, margin=0.10):
    img_input = preprocess(image).unsqueeze(0).to(device)

    with torch.no_grad():
        image_feat = model.encode_image(img_input)
        image_feat /= image_feat.norm(dim=-1, keepdim=True)

        cloth_feat = model.encode_text(CLOTHING_TOKENS)
        cloth_feat /= cloth_feat.norm(dim=-1, keepdim=True)
        cloth_sim = (image_feat @ cloth_feat.T).softmax(dim=-1)
        best_cloth = cloth_sim[0].max().item()

        not_feat = model.encode_text(NOT_TOKENS)
        not_feat /= not_feat.norm(dim=-1, keepdim=True)
        not_sim = (image_feat @ not_feat.T).softmax(dim=-1)
        best_not = not_sim[0].max().item()

    is_clothing = (best_cloth > threshold) and (best_cloth > best_not + margin)

    return {
        "isClothing": is_clothing,
        "clothingScore": best_cloth,
        "notClothingScore": best_not
    }

def predict_is_clothing(image: Image.Image):
    img_input = preprocess(image).unsqueeze(0).to(device)

    with torch.no_grad():
        img_feat = model.encode_image(img_input)
        img_feat /= img_feat.norm(dim=-1, keepdim=True)

        cloth_feat = model.encode_text(CLOTHING_TOKENS)
        cloth_feat /= cloth_feat.norm(dim=-1, keepdim=True)
        cloth_scores = (img_feat @ cloth_feat.T)[0]
        best_cloth = cloth_scores.max().item()

        not_feat = model.encode_text(NOT_TOKENS)
        not_feat /= not_feat.norm(dim=-1, keepdim=True)
        not_scores = (img_feat @ not_feat.T)[0]
        best_not = not_scores.max().item()

    is_clothing = (
        best_cloth > best_not * 1.2    # 옷이 더 높아야 함
        and best_cloth > 0.18          # 최소 confidence
    )

    return {
        "isClothing": is_clothing,
        "clothingScore": float(best_cloth),
        "notClothingScore": float(best_not)
    }

@app.post("/validate")
async def validate(image: UploadFile = File(...)):
    try:
        contents = await image.read()
        img = Image.open(BytesIO(contents)).convert("RGB")

        result = predict_is_clothing(img)

        return result

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))