# Windows 10 배포 가이드

## 생성된 Docker 이미지

다음 Docker 이미지가 생성되었습니다:

- `monitoring-system-web:latest` - Web 애플리케이션
- `monitoring-system-worker:latest` - Worker 애플리케이션

## 압축 파일

배포를 위해 다음 압축 파일이 생성되었습니다:

- `monitoring-system-web-latest.zip` - Web 이미지 압축 파일
- `monitoring-system-worker-latest.zip` - Worker 이미지 압축 파일

## Windows 10에서 배포하기

### 1. 사전 요구사항

- Docker Desktop for Windows 설치
- PostgreSQL 데이터베이스 (로컬 또는 원격)

### 2. 이미지 로드 (다른 서버에서 생성한 경우)

```powershell
# 압축 파일 압축 해제
Expand-Archive -Path monitoring-system-web-latest.zip -DestinationPath . -Force
Expand-Archive -Path monitoring-system-worker-latest.zip -DestinationPath . -Force

# Docker 이미지 로드
docker load -i monitoring-system-web-latest.tar
docker load -i monitoring-system-worker-latest.tar
```

### 3. 환경 변수 설정

`.env` 파일을 생성하여 환경 변수를 설정합니다.

**중요**: `.env` 파일은 `docker-compose.yml`과 **같은 디렉토리**에 위치해야 합니다!

```powershell
# .env.example 파일을 .env로 복사
Copy-Item .env.example .env

# .env 파일을 열어서 실제 값으로 수정
notepad .env
```

자세한 내용은 `ENV_SETUP.md` 파일을 참조하세요.

**중요 설정 항목:**

#### 데이터베이스 연결
```yaml
SPRING_DATASOURCE_URL: jdbc:postgresql://172.22.1.113:5432/monitoring
SPRING_DATASOURCE_USERNAME: monitoring
SPRING_DATASOURCE_PASSWORD: monitoring
```

#### Aligo API 설정 (알림톡/SMS)
```yaml
ALIGO_API_KEY: your-api-key
ALIGO_USER_ID: your-user-id
ALIGO_SENDER: 02-736-3500
ALIGO_SENDER_KEY: your-sender-key
ALIGO_TEMPLATE_CODE: UC_2669
ALIGO_TEST_MODE: N  # Y: 테스트 모드, N: 실제 전송
```

#### 메일 설정
```yaml
SPRING_MAIL_HOST: smtp.gmail.com
SPRING_MAIL_PORT: 587
SPRING_MAIL_USERNAME: your-email@gmail.com
SPRING_MAIL_PASSWORD: your-password
```

#### Worker 설정
```yaml
WORKER_ID: worker-1  # 자동 생성되므로 생략 가능
WORKER_CLAIM_LIMIT: 10
WORKER_LOCK_SECONDS: 30
```

### 4. Docker Compose로 실행

#### 전체 서비스 실행 (PostgreSQL 포함)
```powershell
docker-compose up -d
```

#### PostgreSQL 없이 실행 (외부 DB 사용)
`docker-compose.yml`에서 `postgres` 서비스를 제거하고 실행:

```powershell
docker-compose up -d web worker
```

#### Worker 여러 개 실행
```powershell
docker-compose up -d --scale worker=3
```

### 5. 서비스 확인

#### 컨테이너 상태 확인
```powershell
docker-compose ps
```

#### 로그 확인
```powershell
# Web 로그
docker-compose logs -f web

# Worker 로그
docker-compose logs -f worker

# 모든 로그
docker-compose logs -f
```

#### Web 애플리케이션 접속
브라우저에서 다음 주소로 접속:
```
http://localhost:8080
```

### 6. 서비스 중지 및 제거

```powershell
# 서비스 중지
docker-compose stop

# 서비스 중지 및 컨테이너 제거
docker-compose down

# 볼륨까지 제거 (데이터 삭제 주의!)
docker-compose down -v
```

## 설정 파일 위치

모든 설정 파일은 JAR 파일 내부에 포함되어 있습니다:

- `monitoring-web/src/main/resources/application.yml` - Web 설정
- `monitoring-worker/src/main/resources/application.yml` - Worker 설정

환경 변수로 오버라이드 가능합니다.

## 문제 해결

### 포트 충돌
8080 포트가 이미 사용 중인 경우:
```yaml
# docker-compose.yml
ports:
  - "8081:8080"  # 호스트 포트 변경
```

### 데이터베이스 연결 실패
- PostgreSQL이 실행 중인지 확인
- 방화벽 설정 확인
- `SPRING_DATASOURCE_URL` 환경 변수 확인

### Worker가 실행되지 않음
- Worker 로그 확인: `docker-compose logs worker`
- 데이터베이스 연결 확인
- `WORKER_ID` 환경 변수 확인

## 추가 정보

자세한 내용은 다음 문서를 참조하세요:
- `README.md` - 프로젝트 개요
- `DEPLOY.md` - 배포 가이드
- `QUICK_START.md` - 빠른 시작 가이드
- `TROUBLESHOOTING.md` - 문제 해결 가이드
