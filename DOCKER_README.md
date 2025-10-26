# TryOnXKNU_BE Docker 가이드

## 📋 목차
- [사전 준비](#사전-준비)
- [Docker 설치](#docker-설치)
- [빌드 및 실행](#빌드-및-실행)
- [환경 변수 설정](#환경-변수-설정)
- [트러블슈팅](#트러블슈팅)

## 🔧 사전 준비

### Docker 설치 확인
```bash
docker --version
docker-compose --version
```

## 🐳 Docker 설치

### macOS
1. Docker Desktop 다운로드:
   - https://www.docker.com/products/docker-desktop/
2. DMG 파일 실행 및 Applications 폴더로 이동
3. Docker Desktop 실행
4. 상단 메뉴바에서 Docker 아이콘 확인 (실행 중)

### Windows
1. Docker Desktop 다운로드:
   - https://www.docker.com/products/docker-desktop/
2. 설치 프로그램 실행
3. WSL 2 설치 (필요시)
4. Docker Desktop 실행

### Linux (Ubuntu)
```bash
# Docker 설치
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# Docker Compose 설치
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 현재 사용자를 docker 그룹에 추가
sudo usermod -aG docker $USER
newgrp docker
```

## 🚀 빌드 및 실행

### 방법 1: Docker Compose 사용 (권장)

전체 스택(MySQL, Redis, Application)을 한 번에 실행:

```bash
# 프로젝트 디렉토리로 이동
cd /Users/jkmoi/Desktop/knu_project/tryonXApp/TryOnXKNU_BE

# 백그라운드 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f app

# 상태 확인
docker-compose ps

# 중지
docker-compose down

# 중지 및 볼륨 삭제 (데이터베이스 초기화)
docker-compose down -v
```

### 방법 2: Docker 단독 사용

#### 1. Docker 이미지 빌드
```bash
cd /Users/jkmoi/Desktop/knu_project/tryonXApp/TryOnXKNU_BE
docker build -t tryonx-backend:latest .
```

#### 2. 이미지 확인
```bash
docker images | grep tryonx-backend
```

#### 3. 컨테이너 실행 (MySQL, Redis 별도 준비 필요)
```bash
docker run -d \
  --name tryonx-app \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://host.docker.internal:3306/tryonx \
  -e SPRING_DATASOURCE_USERNAME=tryonx_user \
  -e SPRING_DATASOURCE_PASSWORD=tryonx_pass \
  -e SPRING_DATA_REDIS_HOST=host.docker.internal \
  -e SPRING_DATA_REDIS_PORT=6379 \
  tryonx-backend:latest
```

#### 4. 로그 확인
```bash
docker logs -f tryonx-app
```

#### 5. 컨테이너 접속
```bash
docker exec -it tryonx-app sh
```

#### 6. 컨테이너 중지 및 삭제
```bash
docker stop tryonx-app
docker rm tryonx-app
```

## 🔧 환경 변수 설정

`docker-compose.yml` 파일에서 환경 변수를 수정할 수 있습니다:

### 데이터베이스 설정
```yaml
environment:
  SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/tryonx
  SPRING_DATASOURCE_USERNAME: tryonx_user
  SPRING_DATASOURCE_PASSWORD: tryonx_pass
```

### JWT 설정 (필요시)
```yaml
environment:
  JWT_SECRET: your-secret-key-here
  JWT_EXPIRATION: 86400000
```

### AWS 설정 (필요시)
```yaml
environment:
  CLOUD_AWS_CREDENTIALS_ACCESS_KEY: your-access-key
  CLOUD_AWS_CREDENTIALS_SECRET_KEY: your-secret-key
  CLOUD_AWS_S3_BUCKET: your-bucket-name
  CLOUD_AWS_REGION_STATIC: ap-northeast-2
```

### 메일 설정 (필요시)
```yaml
environment:
  SPRING_MAIL_HOST: smtp.gmail.com
  SPRING_MAIL_PORT: 587
  SPRING_MAIL_USERNAME: your-email@gmail.com
  SPRING_MAIL_PASSWORD: your-app-password
```

## 📊 상태 확인

### 애플리케이션 상태 확인
```bash
# Health check
curl http://localhost:8080/actuator/health

# API 문서 (Swagger)
open http://localhost:8080/swagger-ui.html
```

### 데이터베이스 접속
```bash
# MySQL 컨테이너 접속
docker exec -it tryonx-mysql mysql -u tryonx_user -ptryonx_pass tryonx

# 또는 외부에서 접속
mysql -h 127.0.0.1 -P 3306 -u tryonx_user -ptryonx_pass tryonx
```

### Redis 접속
```bash
# Redis 컨테이너 접속
docker exec -it tryonx-redis redis-cli

# 또는 외부에서 접속
redis-cli -h 127.0.0.1 -p 6379
```

## 🐛 트러블슈팅

### 포트 충돌
```bash
# 사용 중인 포트 확인 (macOS/Linux)
lsof -i :8080
lsof -i :3306
lsof -i :6379

# 프로세스 종료
kill -9 <PID>

# 또는 docker-compose.yml에서 포트 변경
ports:
  - "8081:8080"  # 호스트:컨테이너
```

### 빌드 오류
```bash
# 캐시 없이 다시 빌드
docker-compose build --no-cache app

# 또는
docker build --no-cache -t tryonx-backend:latest .
```

### 데이터베이스 연결 오류
```bash
# MySQL 컨테이너 로그 확인
docker-compose logs mysql

# MySQL 상태 확인
docker-compose exec mysql mysqladmin -u root -proot1234 status

# 데이터베이스 재시작
docker-compose restart mysql
```

### 메모리 부족
```bash
# Docker Desktop에서 메모리 할당 증가:
# Settings > Resources > Memory 를 4GB 이상으로 설정

# 또는 개별 컨테이너에 메모리 제한 추가
docker run -m 1g -d tryonx-backend:latest
```

### 로그 확인
```bash
# 모든 서비스 로그
docker-compose logs

# 특정 서비스 로그
docker-compose logs app
docker-compose logs mysql
docker-compose logs redis

# 실시간 로그 추적
docker-compose logs -f app
```

### 컨테이너 내부 디버깅
```bash
# 애플리케이션 컨테이너 접속
docker exec -it tryonx-app sh

# 프로세스 확인
ps aux

# 네트워크 확인
netstat -tuln

# 환경 변수 확인
env | grep SPRING
```

### 완전 초기화
```bash
# 모든 컨테이너, 네트워크, 볼륨 삭제
docker-compose down -v

# Docker 시스템 정리
docker system prune -a --volumes
```

## 📝 유용한 명령어

```bash
# 실행 중인 컨테이너 확인
docker ps

# 모든 컨테이너 확인
docker ps -a

# 이미지 목록
docker images

# 디스크 사용량 확인
docker system df

# 네트워크 확인
docker network ls

# 볼륨 확인
docker volume ls

# 특정 컨테이너 리소스 사용량 모니터링
docker stats tryonx-app
```

## 🌐 프로덕션 배포

프로덕션 환경에서는:
1. 환경 변수를 `.env` 파일로 분리
2. 민감한 정보는 Docker Secrets 사용
3. HTTPS 설정 (Nginx + Let's Encrypt)
4. 로그 관리 (ELK Stack)
5. 모니터링 설정 (Prometheus + Grafana)
6. 백업 전략 수립

## 📚 참고 자료

- [Docker 공식 문서](https://docs.docker.com/)
- [Docker Compose 문서](https://docs.docker.com/compose/)
- [Spring Boot Docker 가이드](https://spring.io/guides/topicals/spring-boot-docker/)

