# TryOnX — AI Virtual Try-On & E-commerce Platform (Spring Boot)

**TryOnX**는 *AI 가상 피팅 이미지 생성(ComfyUI 연동)* 과 *이커머스 운영 전반(상품/주문/결제/교환/반품/리뷰/문의/공지/회원/관리자 백오피스)* 을 통합한 서비스입니다.  
Spring Boot 백엔드가 외부 AI 실행 환경(ComfyUI, ngrok)을 오케스트레이션하고, 생성 결과 및 각종 이미지를 **AWS S3**에 저장하여 사용자에게 제공하는 구조로 설계했습니다.

🎬 **Demo Video**: https://youtu.be/sDikO-VajOo

---

## Highlights

- **AI Fitting (ComfyUI 연동)**: workflow 템플릿 기반 생성 요청 → `prompt_id` 기반 결과 조회/폴링 → 결과 이미지 S3 업로드/관리
- **Pose 기반 BodyShape 자동 업데이트(WebSocket)**: 실시간 pose 결과를 수신해 유저 체형(BodyShape) 상태 갱신
- **Admin Back-office**: 상품/주문/회원/문의 답변/교환·반품 처리/대시보드/피팅 운영까지 관리자 기능 포함
- **Commerce End-to-End**: 상품·옵션(재고/상태), 장바구니/좋아요, 주문/결제(PortOne), 배송, 리뷰(S3 이미지), 교환/반품(전액/부분 환불, 포인트/재고 복구)
- **Personalized UX**: 인기 스타일(좋아요 Top) & 사용자 체형 기반 유사 스타일 추천

---

## Tech Stack

**Backend**
- Java 17, Spring Boot
- Spring Security, JWT
- Spring Data JPA (Hibernate)
- MySQL, Redis
- WebSocket
- Swagger(OpenAPI)

**Infra / External**
- AWS S3
- Kakao OAuth
- Apple Login (모듈)
- SMTP(Gmail)
- CoolSMS
- PortOne(Iamport)
- ngrok (ComfyUI 연결)

**AI**
- ComfyUI workflow 기반 이미지 생성
- validate_service: FastAPI + (CLIP/Transformers/PyTorch) 기반 의상 이미지 검증 서비스

---

## Core Features

### User Features
- **메인 홈**
  - 인기 스타일(좋아요 Top N) 조회
  - 사용자 BodyShape 기반 유사 스타일 추천 
- **상품**
  - 전체 상품 / 카테고리별 조회 / 상세 조회 
  - 상세에서 리뷰 요약(평점 평균/리뷰 수/미리보기) 및 피팅 이미지 리스트 제공
- **검색**
  - 키워드 기반 검색 (HIDDEN 제외, 할인율 정규화 및 할인 가격 계산) 
- **리뷰**
  - 리뷰 작성(multipart: dto + images) 및 S3 업로드
  - 리뷰 작성 시 적립금 지급(결제 금액 기준 5%) 
- **반품**
  - 반품 신청 / 내역 조회 / 상세 조회 
- **AI 피팅**
  - 1~2벌 착장 생성(dual try-on) 요청 및 결과 조회
  - 결과 이미지를 S3로 저장 후 URL 반환

### Admin Features (Back-office)
- **상품 관리**
  - 상품 생성 시:
    - productCode 자동 생성
    - 상품 이미지 **S3 업로드**
    - 동일 파일명을 **Comfy 서버(/upload/image)**로도 전달하여 AI 파이프라인 입력 동기화 
- **반품 관리**
  - 전체 반품 목록/상세 조회
  - 상태 변경(PATCH) 및 반려 사유 처리 
  - 상태 COMPLETED 시:
    - 주문 금액에 따라 **전액/부분 환불**
    - 사용 포인트 반환 + 포인트 히스토리 적재
    - 재고 복구(상품 아이템 stock + 1)
- (레포에 포함된 다른 관리자 컨트롤러들: 주문/회원/문의/피팅/집계 등)

---

## Architecture Overview

### High-level Flow
1. **Client** → Backend: 로그인/JWT 기반 요청
2. **Backend** → DB(MySQL) / Redis: 도메인 데이터 처리
3. **Backend** → S3: 상품/리뷰/피팅 결과 이미지 업로드 및 URL 관리
4. **Backend** → ComfyUI(ngrok): workflow 요청 및 결과 조회
5. (실시간) **Client** → Backend WebSocket: pose 전송 → BodyShape 업데이트

---

## AI Fitting — Implementation Details

### 1) Pose 기반 BodyShape 자동 업데이트 (WebSocket)
- 클라이언트가 `/ws/pose`로 PoseResult(frameTs, memberId, bodyType, landmarks 등)를 전송
- 서버는 최신 pose를 저장하고, 회원 BodyShape(STRAIGHT/WAVE/NATURAL) 업데이트 로직을 수행

### 2) ComfyUI Orchestration (목적별 서비스 분리)
프로젝트는 ComfyUI 연동을 단일 서비스로 두지 않고, 목적에 맞게 분리했습니다.

- **사용자용 1~2벌 착장 + S3 업로드**
  - 카테고리/체형 기반 모델 이미지 매핑
  - 제약(드레스 단일착용, 동일/그룹 중복 등) 처리
  - `/prompt` 실행 → `/history/{prompt_id}` 조회 → `/view?filename=`로 이미지 바이트 수신 → S3 업로드
- **Dual Output(A/B) 저장 서비스**
  - 출력 prefix 기반(A/B) 탐지 후 저장/DB 고정 시퀀스 처리
- **관리자용 3장(A/B/C) 생성 + S3 업로드 + 시퀀스(1~3) 저장**
  - 워크플로(v2_admin_fitting 등) 기반 3장 생성, prefix 기반 탐지 후 업로드/저장

---

## Commerce Logic — Implementation Details

### 상품 생성: S3 업로드 + Comfy 입력 동기화
- 상품 생성 시 productCode를 만들고,
- 이미지 파일은 UUID를 포함해 S3에 업로드하며,
- 같은 파일명으로 Comfy 서버에도 업로드(`/upload/image`)합니다. 

### 검색/가격 계산
- 할인율이 `10`(%) 형태로 저장되거나 `0.1` 형태일 수 있어 정규화 후 할인 가격을 계산합니다. 

### 리뷰 적립금 + 이미지 S3 업로드
- 리뷰 작성 시 주문 아이템 금액 기준으로 적립금을 지급하고,
- 리뷰 이미지는 S3에 업로드하여 URL을 저장합니다. 

### 반품 처리(환불/포인트/재고)
- 반품 상태 COMPLETED 시:
  - 주문 금액에 따라 전액/부분 환불 분기
  - 사용 포인트 반환 및 히스토리 기록
  - 재고 복구 

---

## API Overview (Examples)

### Main Home
- `GET /api/v1/main/popular-styles` : 인기 스타일(좋아요 Top)  
- `GET /api/v1/main/similar-styles` : 사용자 BodyShape 기반 추천 

### Products
- `GET /api/v1/products` : 전체 상품  
- `GET /api/v1/products/category/{categoryId}`  
- `GET /api/v1/products/{productId}` : 상세 

### Search
- `GET /api/v1/search?keyword=...` : 검색 

### Reviews
- `POST /api/v1/reviews` (multipart: dto + images) : 리뷰 작성 
- `GET /api/v1/reviews` : 내 리뷰 조회 

### Returns
- `POST /api/v1/return` : 반품 신청 
- `GET /api/v1/return/my` : 내 반품 내역 
- `PATCH /api/v1/admin/returns/{returnId}/status/{status}?reason=` : (관리자) 반품 상태 변경 

---

## Configuration

```properties
spring.application.name=TryOnX

# DB
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.url=jdbc:mysql://localhost:3306/TRYONX
spring.datasource.username=YOUR_DB_USERNAME
spring.datasource.password=YOUR_DB_PASSWORD
spring.jpa.hibernate.ddl-auto=update

# JWT
jwt.secretKey=YOUR_JWT_SECRET
jwt.expiration=3000

# Mail (SMTP)
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=YOUR_SMTP_USERNAME
spring.mail.password=YOUR_SMTP_APP_PASSWORD
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# ComfyUI
comfy.input-dir=/content/drive/MyDrive/ComfyUI/input
ngrok.url=YOUR_COMFYUI_BASE_URL

# OAuth (Kakao)
kakao.client.id=YOUR_KAKAO_CLIENT_ID
kakao.redirect.url=http://localhost:8080/kakao/callback
kakao.accesstoken.url=https://kauth.kakao.com/oauth/token
kakao.userinfo.url=https://kapi.kakao.com/v2/user/me

# CoolSMS
coolsms.apiKey=YOUR_COOLSMS_API_KEY
coolsms.apiSecret=YOUR_COOLSMS_API_SECRET
coolsms.number=YOUR_COOLSMS_SENDER_NUMBER

# PortOne (Iamport)
imp.access=YOUR_IMP_ACCESS_KEY
imp.secret=YOUR_IMP_SECRET_KEY

# File upload
spring.servlet.multipart.max-file-size=20MB
spring.servlet.multipart.max-request-size=20MB

# Server
server.address=0.0.0.0
server.port=8080

# WebSocket allowed origins
spring.websocket.allowed-origins=https://*.ngrok-free.app,http://localhost:8080,http://localhost:3000

# Timezone
spring.jpa.properties.hibernate.jdbc.time_zone=Asia/Seoul
spring.jackson.time-zone=Asia/Seoul
spring.jackson.date-format=yyyy.MM.dd HH:mm

# S3
cloud.aws.s3.bucket=YOUR_S3_BUCKET
cloud.aws.stack.auto=false
cloud.aws.region.static=ap-northeast-2
cloud.aws.credentials.accessKey=YOUR_AWS_ACCESS_KEY
cloud.aws.credentials.secretKey=YOUR_AWS_SECRET_KEY
