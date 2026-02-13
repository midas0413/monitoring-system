# Docker 빠른 시작 가이드

## 1분 안에 시작하기

### Windows

```cmd
REM 1. 환경 변수 파일 생성
copy env.example .env

REM 2. .env 파일을 열어서 설정 수정 (선택사항)

REM 3. Docker 시작
docker-start.bat
```

또는 수동으로:

```cmd
docker-compose up -d
```

### Linux/Mac

```bash
# 1. 환경 변수 파일 생성
cp env.example .env

# 2. .env 파일을 열어서 설정 수정 (선택사항)

# 3. Docker 시작
./docker-start.sh
```

또는 수동으로:

```bash
docker-compose up -d
```

## 접속

- **Web UI**: http://localhost:8080
- **PostgreSQL**: localhost:5432

## 필수 설정

`.env` 파일에서 다음 값들을 반드시 수정하세요:

```env
# 데이터베이스 비밀번호 (보안)
SPRING_DATASOURCE_PASSWORD=your-secure-password
POSTGRES_PASSWORD=your-secure-password

# Aligo API (SMS/카카오 알림톡 사용 시)
ALIGO_API_KEY=your-api-key
ALIGO_USER_ID=your-user-id
ALIGO_SENDER=02-0000-0000
ALIGO_SENDER_KEY=your-sender-key
```

## 자주 사용하는 명령어

```bash
# 로그 확인
docker-compose logs -f

# 컨테이너 중지
docker-compose down

# 컨테이너 재시작
docker-compose restart

# 이미지 재빌드
docker-compose build
docker-compose up -d
```

자세한 내용은 [DEPLOY_DOCKER.md](DEPLOY_DOCKER.md)를 참조하세요.
