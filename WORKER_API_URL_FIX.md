# Worker API URL 오타 수정 가이드

## 문제
오류 메시지에 `R172.22.2.13`이 포함되어 있습니다:
```
Worker API 연결 실패: I/O error on POST request for "http://R172.22.2.13:8081/api/worker/test/sql"
```

## 원인
환경변수나 설정에서 IP 주소 앞에 "R"이 붙어있습니다.

## 확인 및 수정 방법

### 1. 시스템 환경변수 확인 (Windows)
```powershell
# 환경변수 확인
$env:WORKER_API_BASE_URL

# 환경변수 설정 (잘못된 값이 있으면 수정)
$env:WORKER_API_BASE_URL = "http://172.22.2.13:8081"
```

### 2. Docker 컨테이너 환경변수 확인
```powershell
# 실행 중인 컨테이너 확인
docker ps

# 컨테이너 환경변수 확인
docker exec monitoring-web env | findstr WORKER_API_BASE_URL

# 또는 docker-compose로 실행 중인 경우
docker-compose exec web env | findstr WORKER_API_BASE_URL
```

### 3. .env 파일 확인
프로젝트 루트에 `.env` 파일이 있다면 확인:
```bash
WORKER_API_BASE_URL=http://172.22.2.13:8081  # R이 없어야 함
```

### 4. application.yml 확인
`monitoring-web/src/main/resources/application.yml` 파일 확인:
```yaml
worker:
  api:
    base-url: ${WORKER_API_BASE_URL:http://172.22.2.13:8081}  # 기본값 확인
```

### 5. Docker Compose 파일 확인
`docker-compose.web.yml` 파일 확인:
```yaml
environment:
  WORKER_API_BASE_URL: ${WORKER_API_BASE_URL:-http://172.22.2.13:8081}  # R이 없어야 함
```

## 수정 방법

### 방법 1: 환경변수 직접 설정
```powershell
# Windows PowerShell
$env:WORKER_API_BASE_URL = "http://172.22.2.13:8081"

# 애플리케이션 재시작
```

### 방법 2: .env 파일 생성/수정
프로젝트 루트에 `.env` 파일 생성:
```bash
WORKER_API_BASE_URL=http://172.22.2.13:8081
```

### 방법 3: Docker Compose 환경변수 수정
`docker-compose.web.yml`에서 직접 수정:
```yaml
environment:
  WORKER_API_BASE_URL: http://172.22.2.13:8081  # 기본값으로 직접 설정
```

### 방법 4: Docker 컨테이너 재시작
```powershell
# 컨테이너 중지
docker-compose down

# 환경변수 설정 후 재시작
$env:WORKER_API_BASE_URL = "http://172.22.2.13:8081"
docker-compose up -d
```

## 확인
수정 후 애플리케이션 로그에서 확인:
```powershell
# Docker 로그 확인
docker logs monitoring-web | findstr "Worker API"

# 또는
docker-compose logs web | findstr "Worker API"
```

로그에 다음과 같이 표시되어야 합니다:
```
Worker API 호출 시작: endpoint=/test/sql, url=http://172.22.2.13:8081/api/worker/test/sql
```

## 주의사항
- IP 주소 앞에 "R"이 붙지 않아야 합니다
- URL은 `http://` 또는 `https://`로 시작해야 합니다
- 포트 번호는 `:8081` 형식이어야 합니다
