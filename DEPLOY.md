# 배포 가이드

다른 서버에 모니터링 시스템을 배포하는 방법을 안내합니다.

## 1. Docker 이미지 생성 (빌드 서버)

### Windows
```cmd
docker-build-and-save.bat [version]
```

### Linux/Mac
```bash
chmod +x docker-build-and-save.sh
./docker-build-and-save.sh [version]
```

예시:
```bash
./docker-build-and-save.sh 1.0.0
```

생성되는 파일:
- Windows: `monitoring-system-web-1.0.0.zip`, `monitoring-system-worker-1.0.0.zip`
- Linux/Mac: `monitoring-system-web-1.0.0.tar.gz`, `monitoring-system-worker-1.0.0.tar.gz`

## 2. 이미지 파일 전송

생성된 압축 파일을 대상 서버로 전송합니다.

```bash
# SCP 사용 예시 (Linux/Mac)
scp monitoring-system-*.tar.gz user@target-server:/path/to/deploy/

# Windows에서 SCP 사용
scp monitoring-system-*.zip user@target-server:/path/to/deploy/

# 또는 FTP, USB 등 다른 방법 사용
```

## 3. 대상 서버에서 이미지 로드

### Windows
```cmd
docker-deploy.bat [version]
```

또는 수동으로:
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

### Linux/Mac
```bash
chmod +x docker-deploy.sh
./docker-deploy.sh [version]
```

예시:
```bash
./docker-deploy.sh latest
```

또는 수동으로:
```bash
# 압축 해제 및 로드
gunzip -c monitoring-system-web-latest.tar.gz | docker load
gunzip -c monitoring-system-worker-latest.tar.gz | docker load
```

## 4. 환경 변수 설정

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

## 5. 서비스 실행

```bash
# 배포용 docker-compose 사용
docker-compose -f docker-compose.deploy.yml up -d

# 또는 환경 변수 파일 사용
docker-compose -f docker-compose.deploy.yml --env-file .env up -d
```

## 6. 데이터베이스 초기화

```bash
# 초기 데이터 삽입
docker-compose -f docker-compose.deploy.yml exec postgres psql -U monitoring -d monitoring -c "$(cat insert_initial_data.sql)"
```

## 7. 로그 확인

```bash
# 모든 서비스 로그
docker-compose -f docker-compose.deploy.yml logs -f

# 개별 서비스 로그
docker-compose -f docker-compose.deploy.yml logs -f web
docker-compose -f docker-compose.deploy.yml logs -f worker
```

## 8. 서비스 중지

```bash
docker-compose -f docker-compose.deploy.yml down

# 볼륨 포함 삭제
docker-compose -f docker-compose.deploy.yml down -v
```

## 필요한 파일 목록

배포 시 다음 파일들을 대상 서버에 복사해야 합니다:

1. **Docker 이미지 파일**
   - `monitoring-system-web-{version}.tar.gz`
   - `monitoring-system-worker-{version}.tar.gz`

2. **Docker Compose 파일**
   - `docker-compose.deploy.yml`
   - `.env` (환경 변수 파일, 선택사항)

3. **스크립트 파일** (선택사항)
   - `docker-deploy.sh` 또는 `docker-deploy.bat`

4. **데이터베이스 초기화 파일** (선택사항)
   - `insert_initial_data.sql`

## 주의사항

1. 대상 서버에 Docker와 Docker Compose가 설치되어 있어야 합니다.
2. 포트 충돌을 확인하세요 (8080, 5432).
3. 환경 변수(특히 데이터베이스 비밀번호, API 키)를 안전하게 관리하세요.
4. 프로덕션 환경에서는 `.env` 파일을 사용하거나 Docker secrets를 활용하세요.
