# VPN 연결 테스트 Worker 통신 확인 가이드

## 개요

VPN 연결 등록/수정 화면에서 연결 테스트 버튼 클릭 시 Worker를 통해 테스트가 수행되는지 확인하는 방법입니다.

## 코드 흐름

### 1. 프론트엔드 (VPN Form)
- **파일**: `monitoring-web/src/main/resources/templates/vpn/form.html`
- **버튼**: `testVpnConnectionBtn` (연결 테스트)
- **API 호출**: `POST /api/checks/test/vpn-connection`
- **파라미터**: `host` (VPN TEST IP)

### 2. Web 컨트롤러
- **파일**: `monitoring-web/src/main/java/com/example/monitoring/web/controller/CheckTestController.java`
- **메서드**: `testVpnConnection(@RequestParam String host)`
- **호출**: `checkTestService.testVpnConnection(host)`

### 3. Web 서비스 (CheckTestService)
- **파일**: `monitoring-web/src/main/java/com/example/monitoring/web/service/CheckTestService.java`
- **메서드**: `testVpnConnection(String host)`
- **로직**:
  1. `workerApiProperties.isEnabled()` 확인
  2. 활성화되어 있으면 `callWorkerApi("/test/vpn-connection", params)` 호출
  3. 비활성화되어 있으면 Web에서 직접 실행 (비권장)

### 4. Worker API 컨트롤러
- **파일**: `monitoring-worker/src/main/java/com/example/monitoring/worker/api/WorkerApiController.java`
- **엔드포인트**: `POST /api/worker/test/vpn-connection`
- **호출**: `workerCheckTestService.testVpnConnection(host)`

### 5. Worker 서비스 (WorkerCheckTestService)
- **파일**: `monitoring-worker/src/main/java/com/example/monitoring/worker/api/WorkerCheckTestService.java`
- **메서드**: `testVpnConnection(String host)`
- **로직**:
  1. Ping 테스트
  2. TCP 연결 테스트 (80, 443, 22, 8080 또는 지정된 포트)

## 로그 확인 방법

### Web 서버 로그

#### 1. VPN 연결 테스트 시작
```
INFO  com.example.monitoring.web.service.CheckTestService - VPN 연결 테스트 시작: host=<VPN_IP>, workerApiEnabled=true
```

#### 2. Worker API 호출 시작
```
INFO  com.example.monitoring.web.service.CheckTestService - Worker API 호출 시작: endpoint=/test/vpn-connection, url=http://<WORKER_URL>/api/worker/test/vpn-connection, params={host=[<VPN_IP>]}
```

#### 3. Worker API 호출 성공
```
INFO  com.example.monitoring.web.service.CheckTestService - Worker API 호출 성공: endpoint=/test/vpn-connection, success=true
```

#### 4. Worker API 연결 실패 (Worker가 실행되지 않은 경우)
```
ERROR com.example.monitoring.web.service.CheckTestService - Worker API 연결 실패: endpoint=/test/vpn-connection, error=Worker API 연결 실패: ...
```

#### 5. Worker API 비활성화 (직접 실행)
```
WARN  com.example.monitoring.web.service.CheckTestService - Worker API가 비활성화되어 있습니다. Web에서 직접 VPN 연결 테스트를 실행합니다: host=<VPN_IP>
```

### Worker 서버 로그

#### 1. VPN 연결 테스트 요청 수신
```
INFO  com.example.monitoring.worker.api.WorkerApiController - VPN 연결 테스트 요청 수신: host=<VPN_IP>
```

#### 2. Worker에서 테스트 시작
```
INFO  com.example.monitoring.worker.api.WorkerCheckTestService - Worker에서 VPN 연결 테스트 시작: host=<VPN_IP>
```

#### 3. Ping 테스트 결과
```
INFO  com.example.monitoring.worker.api.WorkerCheckTestService - Ping 테스트 성공: hostname=<VPN_IP>
또는
WARN  com.example.monitoring.worker.api.WorkerCheckTestService - Ping 테스트 실패: hostname=<VPN_IP>
```

#### 4. TCP 연결 테스트 결과
```
INFO  com.example.monitoring.worker.api.WorkerCheckTestService - TCP 연결 성공: hostname=<VPN_IP>, port=<PORT>
또는
WARN  com.example.monitoring.worker.api.WorkerCheckTestService - TCP 연결 실패: hostname=<VPN_IP>, 모든 포트 시도 실패
```

#### 5. 최종 결과
```
INFO  com.example.monitoring.worker.api.WorkerCheckTestService - VPN 연결 테스트 성공: host=<VPN_IP>, result=...
또는
WARN  com.example.monitoring.worker.api.WorkerCheckTestService - VPN 연결 테스트 실패: host=<VPN_IP>, result=...
```

#### 6. Worker API 컨트롤러 완료
```
INFO  com.example.monitoring.worker.api.WorkerApiController - VPN 연결 테스트 완료: host=<VPN_IP>, success=true
```

## 설정 확인

### Web 서버 설정 (`application.yml`)
```yaml
worker:
  api:
    base-url: ${WORKER_API_BASE_URL:http://localhost:8081}
    enabled: ${WORKER_API_ENABLED:true}  # true여야 Worker를 통해 실행
    api-key: ${WORKER_API_KEY:}  # 선택사항
    timeout-ms: ${WORKER_API_TIMEOUT_MS:10000}
```

### Worker 서버 설정 (`application.yml`)
```yaml
server:
  port: ${WORKER_PORT:8081}

worker:
  api:
    key: ${WORKER_API_KEY:}  # Web과 동일한 키 (선택사항)
```

## 문제 해결

### 1. Worker API가 호출되지 않는 경우
- **확인사항**:
  - `worker.api.enabled=true` 설정 확인
  - `worker.api.base-url` 설정 확인
  - Web 서버 로그에서 "Worker API가 비활성화되어 있습니다" 메시지 확인

### 2. Worker API 연결 실패
- **확인사항**:
  - Worker 서버가 실행 중인지 확인
  - `worker.api.base-url`이 올바른지 확인
  - 방화벽 설정 확인
  - Web 서버 로그에서 "Worker API 연결 실패" 메시지 확인

### 3. Worker에서 테스트가 실행되지 않는 경우
- **확인사항**:
  - Worker 서버 로그에서 "VPN 연결 테스트 요청 수신" 메시지 확인
  - Worker 서버 로그에서 오류 메시지 확인

## 테스트 방법

1. VPN 연결 등록/수정 화면 접속
2. VPN TEST IP 입력
3. "연결 테스트" 버튼 클릭
4. Web 서버 로그 확인:
   - `VPN 연결 테스트 시작` 메시지 확인
   - `Worker API 호출 시작` 메시지 확인 (Worker를 통해 실행되는 경우)
5. Worker 서버 로그 확인:
   - `VPN 연결 테스트 요청 수신` 메시지 확인
   - `Worker에서 VPN 연결 테스트 시작` 메시지 확인
   - 테스트 결과 메시지 확인

## 추가된 로깅

### Web 측
- VPN 연결 테스트 시작/완료 로그
- Worker API 호출 시작/성공/실패 로그
- Worker API 비활성화 경고 로그

### Worker 측
- VPN 연결 테스트 요청 수신 로그
- Worker에서 테스트 시작 로그
- Ping 테스트 결과 로그
- TCP 연결 테스트 결과 로그
- 최종 테스트 결과 로그

이제 로그를 통해 VPN 연결 테스트가 Worker를 통해 실행되는지 명확히 확인할 수 있습니다.
