# Web-Worker 분리 배포 가이드

## 개요

`monitoring-web`과 `monitoring-worker`를 서로 다른 서버에 배포할 때 필요한 설정 가이드입니다.

## 네트워크 요구사항

### 1. Web 서버 → Worker 서버 통신
- **포트**: Worker 서버의 8081 포트 (또는 설정한 포트)
- **프로토콜**: HTTP/HTTPS
- **방향**: Web 서버에서 Worker 서버로의 아웃바운드 연결 필요

### 2. 방화벽 설정
- Worker 서버의 방화벽에서 Web 서버 IP로부터의 8081 포트 접근 허용
- 또는 내부 네트워크에서만 접근 가능하도록 설정

## 설정 방법

### Web 서버 설정 (`monitoring-web`)

`application.yml` 또는 환경 변수로 설정:

```yaml
worker:
  api:
    base-url: http://worker-server-ip:8081  # Worker 서버의 실제 IP/도메인
    enabled: true
    api-key: your-secret-api-key  # Worker와 동일한 키 사용 (선택, 권장)
    timeout-ms: 10000  # API 호출 타임아웃 (밀리초)
```

**환경 변수 예시:**
```bash
WORKER_API_BASE_URL=http://192.168.1.100:8081
WORKER_API_ENABLED=true
WORKER_API_KEY=your-secret-api-key
WORKER_API_TIMEOUT_MS=10000
```

### Worker 서버 설정 (`monitoring-worker`)

`application.yml` 또는 환경 변수로 설정:

```yaml
server:
  port: 8081  # Worker API 포트

worker:
  api:
    key: your-secret-api-key  # Web과 동일한 키 사용 (선택, 권장)
```

**환경 변수 예시:**
```bash
WORKER_PORT=8081
WORKER_API_KEY=your-secret-api-key
```

## 보안 고려사항

### 1. API 키 설정 (권장)
- Web과 Worker에 동일한 API 키를 설정하면 인증이 활성화됩니다
- API 키를 설정하지 않으면 인증 없이 접근 가능합니다 (비권장)

### 2. 네트워크 보안
- 내부 네트워크에서만 Worker API에 접근 가능하도록 방화벽 설정
- 필요시 HTTPS 사용 고려 (추가 설정 필요)

### 3. API 키 생성
- 강력한 랜덤 문자열 사용 권장
- 예: `openssl rand -hex 32`

## 배포 체크리스트

### Web 서버
- [ ] `WORKER_API_BASE_URL` 환경 변수 설정 (Worker 서버 IP/도메인)
- [ ] `WORKER_API_ENABLED=true` 설정
- [ ] `WORKER_API_KEY` 설정 (보안을 위해 권장)
- [ ] Worker 서버로의 네트워크 연결 확인

### Worker 서버
- [ ] `WORKER_PORT` 설정 (기본값: 8081)
- [ ] `WORKER_API_KEY` 설정 (Web과 동일한 값)
- [ ] 방화벽에서 8081 포트 열기 (Web 서버 IP만 허용)
- [ ] Worker 서버가 VPN 네트워크에 접근 가능한지 확인

## 테스트

### 1. 네트워크 연결 테스트
Web 서버에서 Worker 서버로의 연결 확인:
```bash
curl http://worker-server-ip:8081/api/worker/server-status?serverId=1
```

### 2. API 키 테스트
```bash
curl -H "X-Worker-API-Key: your-secret-api-key" \
     http://worker-server-ip:8081/api/worker/server-status?serverId=1
```

### 3. VPN 테스트
Web UI에서 VPN 연결 테스트를 실행하여 Worker API 통신 확인

## 문제 해결

### Connection refused 오류
- Worker 서버가 실행 중인지 확인
- Worker 서버의 포트가 올바르게 열려있는지 확인
- 방화벽 설정 확인
- `WORKER_API_BASE_URL`이 올바른지 확인

### Unauthorized 오류
- `WORKER_API_KEY`가 Web과 Worker에서 동일한지 확인
- API 키가 올바르게 전달되는지 확인 (헤더: `X-Worker-API-Key`)

### 타임아웃 오류
- 네트워크 지연이 큰 경우 `WORKER_API_TIMEOUT_MS` 값을 증가
- Worker 서버의 응답 시간 확인

## Docker 배포 시

### docker-compose.yml 예시
```yaml
services:
  web:
    environment:
      - WORKER_API_BASE_URL=http://worker:8081
      - WORKER_API_KEY=${WORKER_API_KEY}
      - WORKER_API_ENABLED=true
  
  worker:
    environment:
      - WORKER_PORT=8081
      - WORKER_API_KEY=${WORKER_API_KEY}
```

### .env 파일
```env
WORKER_API_KEY=your-secret-api-key-here
```

# 배포 가이드

다른 서버에 모니터링 시스템을 배포하는 방법을 안내합니다.

## 환경설정 포함 배포 이미지 (로컬/단일 서버)

`.env`와 `docker-compose.yml`(또는 `docker-compose.deploy.yml`)을 그대로 사용해 배포하려면:

### 1) 환경 설정

```bash
# .env.example을 복사 후 실제 값으로 수정
copy .env.example .env   # Windows
cp .env.example .env     # Linux/Mac
```

자세한 변수 설명은 [ENV_SETUP.md](ENV_SETUP.md)를 참고하세요.

### 2) 배포용 이미지 빌드

이미지 태그는 `docker-compose.deploy.yml`에서 사용하는 `monitoring-system-web:latest`, `monitoring-system-worker:latest`와 동일하게 빌드됩니다.

**Windows**
```cmd
docker-build.bat
```

**Linux/Mac**
```bash
chmod +x docker-build.sh
./docker-build.sh
# 버전 지정: ./docker-build.sh 1.0.0
```

### 3) 서비스 실행

```bash
docker-compose -f docker-compose.deploy.yml up -d
```

`.env`는 `docker-compose.deploy.yml`에 `env_file: .env`로 지정되어 있어 같은 디렉터리에 두면 자동으로 적용됩니다.  
다른 경로의 env 파일을 쓰려면: `docker-compose -f docker-compose.deploy.yml --env-file .env up -d`

---

## 다른 서버 배포용 단일 ZIP (Windows)

한 번에 빌드·패키징해서 **다른 서버에 복사할 하나의 ZIP**을 만들 때 사용합니다.

### 빌드 서버(Windows)에서

```cmd
build-deploy-zip.bat [버전]
```

예: `build-deploy-zip.bat` 또는 `build-deploy-zip.bat 1.0.0`

**생성 파일:** `monitoring-system-deploy-{버전}.zip`  
내용: web/worker 이미지 ZIP, `docker-compose.deploy.yml`, `.env.example`, `docker-deploy.bat`, `배포_안내.txt`

### 대상 서버(Windows)에서

1. `monitoring-system-deploy-{버전}.zip` 압축 해제
2. **환경 설정:** `copy .env.example .env` 후 `.env` 수정 (DB, Aligo 등)
3. **이미지 로드:** `docker-deploy.bat [버전]`
4. **실행:** `docker-compose -f docker-compose.deploy.yml up -d`

폴더 안의 `배포_안내.txt`에도 같은 순서가 적혀 있습니다.

---

## Web과 Worker를 서로 다른 서버에 배포

**가능합니다.** Web과 Worker는 서로 HTTP로 통신하지 않고, **같은 PostgreSQL DB**만 사용합니다. 따라서 두 서버가 같은 DB에 접속할 수 있으면 분리 배포해도 됩니다.

### 조건

- **DB는 한 곳**에만 두고, Web·Worker 모두 그 DB의 **동일한 접속 정보**(URL/계정/비밀번호)를 사용해야 합니다.
- Worker 서버에서 DB 서버로 **5432 포트 접속**이 가능해야 합니다 (방화벽/보안 그룹 허용).
- 알림(Aligo/메일) 설정은 **Worker 쪽 .env**에만 있으면 됩니다 (알림 전송은 Worker가 담당).

### 배치 예시

| 구분 | 서버 A | 서버 B |
|------|--------|--------|
| **예시 1** | Postgres + Web | Worker |
| **예시 2** | Postgres | Web (서버 C에 Worker) |
| **예시 3** | Postgres + Web | Worker (여러 대 가능) |

### 설정 요약

- **DB가 있는 서버:**  
  - Postgres만 띄우거나, Postgres + Web을 같은 서버에 띄웁니다.  
  - 다른 서버에서 접속할 수 있도록 `POSTGRES_PORT`를 열어두고, DB 접속용 계정을 맞춥니다.

- **Web만 있는 서버:**  
  - `docker-compose.deploy.yml`에서 `postgres`, `worker` 서비스를 제거하거나,  
  - Web 전용 compose를 만들어 **SPRING_DATASOURCE_URL**을 DB 서버 주소로 설정합니다.  
  - 예: `SPRING_DATASOURCE_URL=jdbc:postgresql://서버A주소:5432/monitoring`

- **Worker만 있는 서버:**  
  - Worker 전용 compose를 만들고, **SPRING_DATASOURCE_URL**을 DB 서버 주소로 설정합니다.  
  - Aligo/메일 관련 환경 변수는 이 서버의 `.env`에 넣습니다.

이렇게 하면 Web은 서버 A, Worker는 서버 B처럼 **서로 다른 서버에 나눠 배포**해도 됩니다.

---

## 1. Docker 이미지 생성 (다른 서버 전송용, 파일 2개)

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

2. **Docker Compose 및 환경 설정**
   - `docker-compose.deploy.yml`
   - `.env` (실제 값으로 채운 환경 변수 파일)
   - `.env.example` (참고용 템플릿, Git 포함)

3. **스크립트 파일** (선택사항)
   - `docker-deploy.sh` 또는 `docker-deploy.bat`

4. **데이터베이스 초기화 파일** (선택사항)
   - `insert_initial_data.sql`

## 주의사항

1. 대상 서버에 Docker와 Docker Compose가 설치되어 있어야 합니다.
2. 포트 충돌을 확인하세요 (8080, 5432).
3. 환경 변수(특히 데이터베이스 비밀번호, API 키)를 안전하게 관리하세요.
4. 프로덕션 환경에서는 `.env` 파일을 사용하거나 Docker secrets를 활용하세요.
