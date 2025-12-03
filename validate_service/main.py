# from fastapi import FastAPI, UploadFile, File, HTTPException
# from fastapi.middleware.cors import CORSMiddleware
# from transformers import CLIPProcessor, CLIPModel
# from PIL import Image
# import torch
# import io
#
# app = FastAPI(title="TryOnX AI Validate Service")
#
# app.add_middleware(
#     CORSMiddleware,
#     allow_origins=["*"],
#     allow_credentials=True,
#     allow_methods=["*"],
#     allow_headers=["*"],
# )
#
# # 1. 모델 및 프로세서 로드 (서버 시작 시 1회 로드)
# # "openai/clip-vit-base-patch32"는 성능과 속도의 균형이 좋은 모델입니다.
# MODEL_ID = "openai/clip-vit-base-patch32"
# print(f"Loading AI Model: {MODEL_ID}...")
# device = "cuda" if torch.cuda.is_available() else "cpu"
# model = CLIPModel.from_pretrained(MODEL_ID).to(device)
# processor = CLIPProcessor.from_pretrained(MODEL_ID)
# print("Model loaded successfully.")
#
# # 2. 검증할 카테고리 정의 (텍스트 프롬프트)
# # 의상인지 아닌지를 판별하기 위한 기준 텍스트입니다.
# LABELS = [
#     "a photo of a clothing garment",   # 의상 (셔츠, 바지, 원피스 등)
#     "a photo of a human face",         # 얼굴 (피팅 부적합)
#     "a photo of a naked body part",    # 과도한 노출 (피팅 부적합)
#     "a complex background with no object", # 배경만 있는 경우
#     "text or document"                 # 텍스트/문서
# ]
#
# @app.get("/")
# async def root():
#     return {"status": "ok", "model": MODEL_ID, "device": device}
#
# @app.post("/validate")
# async def validate(image: UploadFile = File(...)):
#     """
#     CLIP 모델 기반 이미지 검증
#     - 이미지가 '의상(clothing)' 텍스트와 얼마나 유사한지 확률 계산
#     """
#     try:
#         # 이미지 읽기
#         contents = await image.read()
#         if len(contents) == 0:
#             return {"isClothing": False, "confidence": 0.0}
#
#         pil_image = Image.open(io.BytesIO(contents)).convert("RGB")
#
#         # 전처리 및 모델 추론
#         inputs = processor(
#             text=LABELS,
#             images=pil_image,
#             return_tensors="pt",
#             padding=True
#         ).to(device)
#
#         with torch.no_grad():
#             outputs = model(**inputs)
#
#         # 텍스트-이미지 유사도 점수 (Logits) -> 확률(Softmax) 변환
#         logits_per_image = outputs.logits_per_image
#         probs = logits_per_image.softmax(dim=1) # 예: [[0.85, 0.10, 0.02, ...]]
#
#         # 결과 해석
#         # LABELS[0] ("a photo of a clothing garment")의 확률 가져오기
#         clothing_score = probs[0][0].item()
#
#         # 얼굴이나 노출 등 부정적 요소의 점수가 가장 높은지 확인
#         # (clothing 점수가 1등이어야 함)
#         max_idx = probs.argmax().item()
#         is_clothing_top_rank = (max_idx == 0)
#
#         # 결정 로직
#         # 1) 의상일 확률이 가장 높아야 함 (is_clothing_top_rank)
#         # 2) 그 확률이 최소 0.6 (60%) 이상이어야 함
#         threshold = 0.6
#         is_valid = is_clothing_top_rank and (clothing_score > threshold)
#
#         return {
#             "isClothing": is_valid,
#             "confidence": round(clothing_score, 4),
#             "details": {
#                 "top_prediction": LABELS[max_idx],
#                 "scores": {label: round(probs[0][i].item(), 4) for i, label in enumerate(LABELS)}
#             }
#         }
#
#     except Exception as e:
#         print(f"Validation Error: {str(e)}")
#         raise HTTPException(status_code=500, detail="Model inference failed")

from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from transformers import CLIPProcessor, CLIPModel
from PIL import Image
import torch
import io

app = FastAPI(title="TryOnX AI Validate Service")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

MODEL_ID = "openai/clip-vit-base-patch32"
print(f"Loading AI Model: {MODEL_ID}...")
device = "cuda" if torch.cuda.is_available() else "cpu"
model = CLIPModel.from_pretrained(MODEL_ID).to(device)
processor = CLIPProcessor.from_pretrained(MODEL_ID)
print("Model loaded successfully.")

# ==========================================
# [핵심 수정 1] 라벨 정의를 정교하게 변경
# ==========================================
LABELS = [
    "a photo of a standalone clothing garment without person",  # 0번: 의상 단독 (정답)
    "a photo of a person wearing clothes",                    # 1번: 옷 입은 사람 (탈락)
    "a photo of a human face or selfie",                      # 2번: 얼굴/셀카 (탈락)
    "an anime or cartoon character",                          # 3번: 캐릭터 (탈락)
    "a photo of a naked body part",                           # 4번: 노출 (탈락)
    "text or document or blurry background"                   # 5번: 기타 잡음 (탈락)
]

@app.get("/")
async def root():
    return {"status": "ok", "model": MODEL_ID}

@app.post("/validate")
async def validate(image: UploadFile = File(...)):
    try:
        contents = await image.read()
        if len(contents) == 0:
            return {"isClothing": False, "confidence": 0.0}

        pil_image = Image.open(io.BytesIO(contents)).convert("RGB")

        inputs = processor(
            text=LABELS,
            images=pil_image,
            return_tensors="pt",
            padding=True
        ).to(device)

        with torch.no_grad():
            outputs = model(**inputs)

        logits_per_image = outputs.logits_per_image
        probs = logits_per_image.softmax(dim=1)

        # 각 항목별 확률 추출
        scores = {label: round(probs[0][i].item(), 4) for i, label in enumerate(LABELS)}

        # 0번(정답) 점수
        clothing_score = probs[0][0].item()

        # 1등이 누구인지 확인
        max_idx = probs.argmax().item()

        # ==========================================
        # [핵심 수정 2] 검증 로직 강화
        # ==========================================
        # 조건 1: 가장 높은 확률이 0번(단독 의상)이어야 함
        is_top_rank = (max_idx == 0)

        # 조건 2: '옷 입은 사람'일 확률이 너무 높으면 안 됨 (이중 체크)
        # 예: 단독 의상 45%, 옷 입은 사람 40% -> 이런 애매한 경우를 거르기 위함
        person_score = probs[0][1].item() # 옷 입은 사람 점수
        character_score = probs[0][3].item() # 캐릭터 점수

        # "사람"이나 "캐릭터"일 확률이 20%를 넘으면 위험군으로 간주하여 탈락시킬 수도 있음
        # 여기서는 단순히 1등이 아니면 탈락, 그리고 정답 점수가 0.5(50%) 이상이어야 함으로 설정
        threshold = 0.5

        # 최종 판단: 1등이 '단독 의상'이고, 점수가 임계값을 넘어야 함
        is_valid = is_top_rank and (clothing_score > threshold)

        # 로그 출력 (디버깅용)
        print(f"🔍 분석: {LABELS[max_idx]} ({round(probs[0][max_idx].item(), 2)})")
        if not is_valid:
             print(f"   ❌ 탈락 사유: 1등 아님 혹은 점수 미달 (의상점수: {clothing_score:.2f}, 사람점수: {person_score:.2f})")

        return {
            "isClothing": is_valid,
            "confidence": round(clothing_score, 4),
            "details": {
                "top_prediction": LABELS[max_idx],
                "scores": scores
            }
        }

    except Exception as e:
        print(f"Validation Error: {str(e)}")
        raise HTTPException(status_code=500, detail="Model inference failed")
