# 알림규칙 테스트 Worker 통신 확인 가이드

## 개요

알림규칙의 SQL, 로그, 디스크, Shell script 테스트가 Worker를 통해 실행되는지 확인하는 방법입니다.

## 테스트 유형별 확인

### 1. Shell Script 테스트

#### 코드 흐름
1. **프론트엔드**: 알림규칙 등록/수정 화면에서 "테스트" 버튼 클릭
2. **API 호출**: `POST /api/checks/test/shell`
3. **Web 서비스**: `CheckTestService.testShell()` → Worker API 호출
4. **Worker API**: `POST /api/worker/test/shell`
5. **Worker 서비스**: `WorkerCheckTestService.testShell()` 실행

#### 로그 확인

**Web 서버:**
```
INFO  CheckTestService - Shell script 테스트 시작: host=..., workerApiEnabled=true
INFO  CheckTestService - Worker API를 통해 Shell script 테스트 실행: host=...
INFO  CheckTestService - Worker API 호출 시작: endpoint=/test/shell, url=...
INFO  CheckTestService - Worker API 호출 성공: endpoint=/test/shell, success=true
INFO  CheckTestService - Shell script 테스트 완료 (Worker API): host=..., success=true
```

**Worker 서버:**
```
INFO  WorkerApiController - Shell script 테스트 요청 수신: host=...
INFO  WorkerCheckTestService - Worker에서 Shell script 테스트 시작: host=..., port=...
INFO  WorkerCheckTestService - Shell script 테스트 성공: host=..., port=...
INFO  WorkerApiController - Shell script 테스트 완료: host=..., success=true
```

### 2. SQL 테스트

#### 코드 흐름
1. **프론트엔드**: 알림규칙 등록/수정 화면에서 "테스트" 버튼 클릭
2. **API 호출**: `POST /api/checks/test/sql`
3. **Web 서비스**: `CheckTestService.testSql()` → Worker API 호출
4. **Worker API**: `POST /api/worker/test/sql`
5. **Worker 서비스**: `WorkerCheckTestService.testSql()` 실행

#### 로그 확인

**Web 서버:**
```
INFO  CheckTestService - SQL 테스트 시작: host=..., dbType=..., dbName=..., workerApiEnabled=true
INFO  CheckTestService - Worker API를 통해 SQL 테스트 실행: host=..., dbType=..., dbName=...
INFO  CheckTestService - Worker API 호출 시작: endpoint=/test/sql, url=...
INFO  CheckTestService - Worker API 호출 성공: endpoint=/test/sql, success=true
INFO  CheckTestService - SQL 테스트 완료 (Worker API): host=..., success=true
```

**Worker 서버:**
```
INFO  WorkerApiController - SQL 테스트 요청 수신: host=..., dbType=..., dbName=...
INFO  WorkerCheckTestService - Worker에서 SQL 테스트 시작: host=..., dbType=..., dbName=...
INFO  WorkerCheckTestService - SQL 테스트 성공: host=..., dbType=..., dbName=..., result=...
INFO  WorkerApiController - SQL 테스트 완료: host=..., success=true
```

### 3. 로그 파일 테스트

#### 코드 흐름
1. **프론트엔드**: 알림규칙 등록/수정 화면에서 "테스트" 버튼 클릭
2. **API 호출**: `POST /api/checks/test/logs`
3. **Web 서비스**: `CheckTestService.testLogs()` → Worker API 호출
4. **Worker API**: `POST /api/worker/test/logs`
5. **Worker 서비스**: `WorkerCheckTestService.testLogs()` 실행

#### 로그 확인

**Web 서버:**
```
INFO  CheckTestService - 로그 파일 테스트 시작: host=..., logFilePath=..., workerApiEnabled=true
INFO  CheckTestService - Worker API를 통해 로그 파일 테스트 실행: host=..., logFilePath=...
INFO  CheckTestService - Worker API 호출 시작: endpoint=/test/logs, url=...
INFO  CheckTestService - Worker API 호출 성공: endpoint=/test/logs, success=true
INFO  CheckTestService - 로그 파일 테스트 완료 (Worker API): host=..., success=true
```

**Worker 서버:**
```
INFO  WorkerApiController - 로그 파일 테스트 요청 수신: host=..., logFilePath=...
INFO  WorkerCheckTestService - Worker에서 로그 파일 테스트 시작: host=..., logFilePath=...
INFO  WorkerCheckTestService - 로그 파일 테스트 성공: host=..., logFilePath=...
INFO  WorkerApiController - 로그 파일 테스트 완료: host=..., success=true
```

### 4. 디스크 공간 테스트

#### 코드 흐름
1. **프론트엔드**: 알림규칙 등록/수정 화면에서 "테스트" 버튼 클릭
2. **API 호출**: `POST /api/checks/test/disk-space`
3. **Web 서비스**: `CheckTestService.testDiskSpace()` → Worker API 호출
4. **Worker API**: `POST /api/worker/test/disk-space`
5. **Worker 서비스**: `WorkerCheckTestService.testDiskSpace()` 실행

#### 로그 확인

**Web 서버:**
```
INFO  CheckTestService - 디스크 공간 테스트 시작: host=..., diskPath=..., workerApiEnabled=true
INFO  CheckTestService - Worker API를 통해 디스크 공간 테스트 실행: host=..., diskPath=...
INFO  CheckTestService - Worker API 호출 시작: endpoint=/test/disk-space, url=...
INFO  CheckTestService - Worker API 호출 성공: endpoint=/test/disk-space, success=true
INFO  CheckTestService - 디스크 공간 테스트 완료 (Worker API): host=..., success=true
```

**Worker 서버:**
```
INFO  WorkerApiController - 디스크 공간 테스트 요청 수신: host=..., diskPath=...
INFO  WorkerCheckTestService - Worker에서 디스크 공간 테스트 시작: host=..., diskPath=...
INFO  WorkerCheckTestService - 디스크 공간 테스트 성공: host=..., diskPath=...
INFO  WorkerApiController - 디스크 공간 테스트 완료: host=..., success=true
```

### 5. SSH 연결 테스트

#### 코드 흐름
1. **프론트엔드**: 서버 등록/수정 화면에서 "연결 테스트" 버튼 클릭
2. **API 호출**: `POST /api/checks/test/ssh-connection`
3. **Web 서비스**: `CheckTestService.testSshConnection()` → Worker API 호출
4. **Worker API**: `POST /api/worker/test/ssh-connection`
5. **Worker 서비스**: `WorkerCheckTestService.testSshConnection()` 실행

#### 로그 확인

**Web 서버:**
```
INFO  CheckTestService - SSH 연결 테스트 시작: host=..., workerApiEnabled=true
INFO  CheckTestService - Worker API를 통해 SSH 연결 테스트 실행: host=...
INFO  CheckTestService - Worker API 호출 시작: endpoint=/test/ssh-connection, url=...
INFO  CheckTestService - Worker API 호출 성공: endpoint=/test/ssh-connection, success=true
INFO  CheckTestService - SSH 연결 테스트 완료 (Worker API): host=..., success=true
```

**Worker 서버:**
```
INFO  WorkerApiController - SSH 연결 테스트 요청 수신: host=...
INFO  WorkerCheckTestService - Worker에서 SSH 연결 테스트 시작: host=..., port=...
INFO  WorkerCheckTestService - SSH 연결 테스트 성공: host=..., port=...
INFO  WorkerApiController - SSH 연결 테스트 완료: host=..., success=true
```

## 공통 확인 사항

### Worker API 비활성화 시
**Web 서버 로그:**
```
WARN  CheckTestService - Worker API가 비활성화되어 있습니다. Web에서 직접 [테스트 유형] 테스트를 실행합니다: host=...
```

### Worker API 연결 실패 시
**Web 서버 로그:**
```
ERROR CheckTestService - Worker API 연결 실패: endpoint=/test/[테스트 유형], error=...
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

## 테스트 방법

1. 알림규칙 등록/수정 화면 접속
2. 각 테스트 유형에 맞는 정보 입력:
   - **Shell Script**: Host, SSH 정보, 스크립트 입력 후 "테스트" 버튼 클릭
   - **SQL**: Host, DB 정보, SQL 입력 후 "테스트" 버튼 클릭
   - **로그**: Host, SSH 정보, 로그 파일 경로 입력 후 "테스트" 버튼 클릭
   - **디스크**: Host, SSH 정보, 디스크 경로(선택) 입력 후 "테스트" 버튼 클릭
3. Web 서버 로그 확인:
   - `[테스트 유형] 테스트 시작` 메시지 확인
   - `Worker API를 통해 [테스트 유형] 테스트 실행` 메시지 확인
   - `Worker API 호출 시작` 메시지 확인
   - `Worker API 호출 성공` 메시지 확인
4. Worker 서버 로그 확인:
   - `[테스트 유형] 테스트 요청 수신` 메시지 확인
   - `Worker에서 [테스트 유형] 테스트 시작` 메시지 확인
   - 테스트 결과 메시지 확인

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
  - Worker 서버 로그에서 "[테스트 유형] 테스트 요청 수신" 메시지 확인
  - Worker 서버 로그에서 오류 메시지 확인

## 추가된 로깅

### Web 측 (CheckTestService)
- 각 테스트 유형별 시작/완료 로그
- Worker API 호출 시작/성공/실패 로그
- Worker API 비활성화 경고 로그

### Worker 측 (WorkerApiController, WorkerCheckTestService)
- 각 테스트 유형별 요청 수신 로그
- Worker에서 테스트 시작 로그
- 테스트 성공/실패 로그
- 최종 테스트 결과 로그

이제 로그를 통해 모든 알림규칙 테스트가 Worker를 통해 실행되는지 명확히 확인할 수 있습니다.
