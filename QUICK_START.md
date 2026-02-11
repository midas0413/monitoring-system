# 빠른 시작 가이드

## Docker 이미지 배포 (다른 서버)

### 1단계: 이미지 파일 확인

현재 디렉토리에 다음 파일들이 생성되어 있어야 합니다:
- Windows: `monitoring-system-web-latest.zip`, `monitoring-system-worker-latest.zip`
- Linux/Mac: `monitoring-system-web-latest.tar.gz`, `monitoring-system-worker-latest.tar.gz`

### 2단계: 대상 서버로 파일 전송

```bash
# SCP 사용 예시
scp monitoring-system-*.tar.gz user@target-server:/path/to/deploy/

# 또는 FTP, USB 등 다른 방법 사용
```

### 3단계: 대상 서버에서 이미지 로드

#### Windows
```cmd
# 압축 해제 (zip 파일인 경우)
powershell -Command "Expand-Archive -Path monitoring-system-web-latest.zip -DestinationPath . -Force"
powershell -Command "Expand-Archive -Path monitoring-system-worker-latest.zip -DestinationPath . -Force"

# 이미지 로드
docker load -i monitoring-system-web-latest.tar
docker load -i monitoring-system-worker-latest.tar

# 임시 파일 삭제
del monitoring-system-*-latest.tar
```

#### Linux/Mac
```bash
# 압축 해제 및 로드
gunzip -c monitoring-system-web-latest.tar.gz | docker load
gunzip -c monitoring-system-worker-latest.tar.gz | docker load
```

### 4단계: docker-compose.deploy.yml 준비

`docker-compose.deploy.yml` 파일을 대상 서버에 복사합니다.

### 5단계: 환경 변수 설정

`.env` 파일을 생성하거나 환경 변수를 설정합니다:

```bash
# .env 파일 예시
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/monitoring
SPRING_DATASOURCE_USERNAME=monitoring
SPRING_DATASOURCE_PASSWORD=your_password
ALIGO_API_KEY=your_api_key
ALIGO_USER_ID=your_user_id
# ... 기타 환경 변수
```

### 6단계: 서비스 실행

```bash
docker-compose -f docker-compose.deploy.yml up -d
```

### 7단계: 로그 확인

```bash
docker-compose -f docker-compose.deploy.yml logs -f
```

### 8단계: 데이터베이스 초기화 (선택사항)

```bash
# 초기 데이터 삽입
docker-compose -f docker-compose.deploy.yml exec postgres psql -U monitoring -d monitoring -c "$(cat insert_initial_data.sql)"
```

## 문제 해결

### 이미지가 로드되지 않는 경우
- Docker가 실행 중인지 확인: `docker ps`
- 파일이 손상되지 않았는지 확인: 파일 크기 확인
- 디스크 공간 확인: `docker system df`

### 서비스가 시작되지 않는 경우
- 로그 확인: `docker-compose -f docker-compose.deploy.yml logs`
- 환경 변수 확인: `.env` 파일 또는 환경 변수 설정
- 포트 충돌 확인: `netstat -an | findstr 8080` (Windows) 또는 `netstat -tuln | grep 8080` (Linux)

### 데이터베이스 연결 실패
- PostgreSQL 컨테이너가 실행 중인지 확인: `docker ps | grep postgres`
- 데이터베이스 연결 정보 확인: `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
