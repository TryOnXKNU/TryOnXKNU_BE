from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.middleware.cors import CORSMiddleware

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

        # 간단한 heuristic: 아주 작은 파일은 의상이 아님
        if size <= 0:
            return {"isClothing": False, "confidence": 0.0}

        # 큰 파일일수록 신뢰도 높게 (임시 로직)
        confidence = min(0.999, max(0.05, size / (1024 * 200)))
        is_clothing = confidence > 0.18

        return {"isClothing": is_clothing, "confidence": float(confidence)}

    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
