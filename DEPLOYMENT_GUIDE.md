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
