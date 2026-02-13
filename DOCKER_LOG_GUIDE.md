# Docker 로그 확인 가이드

## Docker Compose를 사용하는 경우

### 1. 모든 서비스 로그 확인 (실시간)

```powershell
docker-compose logs -f
```

- `-f` 또는 `--follow`: 실시간으로 로그를 계속 출력 (Ctrl+C로 종료)

### 2. 특정 서비스 로그만 확인

**Web 서비스:**
```powershell
docker-compose logs -f web
```

**Worker 서비스:**
```powershell
docker-compose logs -f worker
```

**PostgreSQL 서비스:**
```powershell
docker-compose logs -f postgres
```

### 3. 최근 로그만 확인

**최근 100줄:**
```powershell
docker-compose logs --tail=100
```

**최근 50줄 (실시간):**
```powershell
docker-compose logs --tail=50 -f
```

### 4. 특정 시간 이후 로그 확인

```powershell
docker-compose logs --since 2024-01-01T00:00:00
```

또는 상대 시간:
```powershell
docker-compose logs --since 10m    # 최근 10분
docker-compose logs --since 1h     # 최근 1시간
docker-compose logs --since 1d     # 최근 1일
```

### 5. 특정 키워드 검색

**PowerShell에서:**
```powershell
docker-compose logs web | Select-String "ERROR"
docker-compose logs worker | Select-String "Worker API"
docker-compose logs | Select-String "Worker API 호출"
```

**CMD에서:**
```cmd
docker-compose logs web | findstr "ERROR"
docker-compose logs worker | findstr "Worker API"
```

### 6. 로그를 파일로 저장

```powershell
docker-compose logs > docker-logs.txt
docker-compose logs web > web-logs.txt
docker-compose logs worker > worker-logs.txt
```

## 개별 컨테이너를 실행하는 경우

### 1. 컨테이너 이름으로 로그 확인

```powershell
docker logs monitoring-web
docker logs monitoring-worker
docker logs monitoring-postgres
```

### 2. 실시간 로그 확인

```powershell
docker logs -f monitoring-web
docker logs -f monitoring-worker
```

### 3. 최근 로그만 확인

```powershell
docker logs --tail=100 monitoring-web
docker logs --tail=50 -f monitoring-worker
```

### 4. 특정 시간 이후 로그

```powershell
docker logs --since 10m monitoring-web
docker logs --since 1h monitoring-worker
```

### 5. 타임스탬프 포함

```powershell
docker logs -t monitoring-web
docker logs -t -f monitoring-worker
```

## 유용한 로그 확인 명령어

### 1. 에러 로그만 확인

```powershell
docker-compose logs | Select-String -Pattern "ERROR|Exception|Failed"
```

### 2. Worker API 호출 로그 확인

```powershell
docker-compose logs web | Select-String "Worker API"
docker-compose logs worker | Select-String "Worker API"
```

### 3. 시작 로그 확인

```powershell
docker-compose logs --tail=200 | Select-String "Started|Application"
```

### 4. 데이터베이스 연결 로그 확인

```powershell
docker-compose logs web | Select-String "DataSource|Database"
docker-compose logs worker | Select-String "DataSource|Database"
```

### 5. 알림 발송 로그 확인

```powershell
docker-compose logs worker | Select-String "Aligo|Notification|알림"
```

## 컨테이너 상태 확인

### 실행 중인 컨테이너 확인

```powershell
docker-compose ps
```

또는:
```powershell
docker ps
```

### 컨테이너 상세 정보

```powershell
docker inspect monitoring-web
docker inspect monitoring-worker
```

## 로그 레벨 변경

로그 레벨을 변경하려면 `.env` 파일에서 설정:

```env
LOGGING_LEVEL_ROOT=DEBUG
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_WEB=DEBUG
```

그리고 컨테이너 재시작:
```powershell
docker-compose restart web worker
```

## 문제 해결

### 로그가 보이지 않는 경우

1. 컨테이너가 실행 중인지 확인:
   ```powershell
   docker-compose ps
   ```

2. 컨테이너가 정지된 경우 로그 확인:
   ```powershell
   docker-compose logs web
   docker-compose logs worker
   ```

3. 컨테이너 재시작:
   ```powershell
   docker-compose restart web worker
   ```

### 로그가 너무 많은 경우

1. 특정 서비스만 확인
2. `--tail` 옵션으로 최근 로그만 확인
3. `Select-String` 또는 `findstr`로 필터링

## 실전 예제

### 1. Web 서비스 시작 로그 확인

```powershell
docker-compose logs --tail=100 web | Select-String "Started|ERROR|Exception"
```

### 2. Worker API 통신 로그 확인

```powershell
docker-compose logs -f web worker | Select-String "Worker API"
```

### 3. 에러 발생 시 전체 로그 확인

```powershell
docker-compose logs --tail=500 > error-logs.txt
notepad error-logs.txt
```

### 4. 실시간 모니터링 (두 터미널 사용)

**터미널 1 - Web 로그:**
```powershell
docker-compose logs -f web
```

**터미널 2 - Worker 로그:**
```powershell
docker-compose logs -f worker
```

## 로그 파일 위치

Docker는 기본적으로 컨테이너의 stdout/stderr를 로그로 저장합니다. 로그는 Docker 데몬에 의해 관리되며, 다음 위치에서 확인할 수 있습니다:

**Windows:**
```
C:\ProgramData\docker\containers\<container-id>\<container-id>-json.log
```

하지만 일반적으로는 `docker logs` 또는 `docker-compose logs` 명령어를 사용하는 것이 더 편리합니다.
