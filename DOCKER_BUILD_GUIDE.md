# Docker 이미지 생성 가이드

## 빠른 시작

### Windows

```cmd
.\docker-build-and-save.bat [version]
```

예시:
```cmd
.\docker-build-and-save.bat latest
.\docker-build-and-save.bat 1.0.0
```

### Linux/Mac

```bash
chmod +x docker-build-and-save.sh
./docker-build-and-save.sh [version]
```

## 수동 빌드 명령어

### 1. Web 이미지 빌드

```powershell
# Windows PowerShell
docker build -f Dockerfile.web -t monitoring-system-web:latest .

# 또는 특정 버전으로
docker build -f Dockerfile.web -t monitoring-system-web:1.0.0 .
```

### 2. Worker 이미지 빌드

```powershell
# Windows PowerShell
docker build -f Dockerfile.worker -t monitoring-system-worker:latest .

# 또는 특정 버전으로
docker build -f Dockerfile.worker -t monitoring-system-worker:1.0.0 .
```

### 3. 이미지 저장 (다른 서버 배포용)

```powershell
# Web 이미지 저장
docker save monitoring-system-web:latest -o monitoring-system-web-latest.tar

# Worker 이미지 저장
docker save monitoring-system-worker:latest -o monitoring-system-worker-latest.tar

# 압축 (Windows)
Compress-Archive -Path monitoring-system-web-latest.tar -DestinationPath monitoring-system-web-latest.zip -Force
Compress-Archive -Path monitoring-system-worker-latest.tar -DestinationPath monitoring-system-worker-latest.zip -Force

# 임시 파일 삭제
Remove-Item monitoring-system-*-latest.tar
```

## Docker Compose를 사용한 빌드

### 빌드만 수행

```powershell
# 모든 서비스 빌드
docker-compose build

# 특정 서비스만 빌드
docker-compose build web
docker-compose build worker

# 캐시 없이 빌드
docker-compose build --no-cache
```

### 빌드 및 실행

```powershell
# 빌드 후 실행
docker-compose up -d --build

# 특정 서비스만 빌드 후 실행
docker-compose up -d --build web
```

## 환경 설정 포함 방법

**중요**: Docker 이미지 자체에는 환경 변수가 포함되지 않습니다. 환경 변수는 컨테이너 실행 시 주입됩니다.

### 방법 1: .env 파일 사용 (권장)

1. **환경 변수 파일 생성**
   ```powershell
   copy env.example .env
   ```

2. **.env 파일 수정**
   ```env
   SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/monitoring
   SPRING_DATASOURCE_USERNAME=monitoring
   SPRING_DATASOURCE_PASSWORD=your-password
   ALIGO_API_KEY=your-api-key
   # ... 기타 설정
   ```

3. **Docker Compose 실행**
   ```powershell
   docker-compose up -d
   ```

### 방법 2: docker-compose.yml에 직접 설정

`docker-compose.yml`의 `environment` 섹션에 직접 설정:

```yaml
services:
  web:
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/monitoring
      ALIGO_API_KEY: your-api-key
      # ...
```

### 방법 3: 환경 변수로 전달

```powershell
# PowerShell에서 환경 변수 설정
$env:SPRING_DATASOURCE_PASSWORD = "your-password"
$env:ALIGO_API_KEY = "your-api-key"

# Docker Compose 실행
docker-compose up -d
```

## 전체 빌드 및 배포 스크립트

### Windows (docker-build-and-save.bat)

```cmd
@echo off
REM Docker 이미지 빌드 및 저장 스크립트

set VERSION=%1
if "%VERSION%"=="" set VERSION=latest

echo ==========================================
echo Building Docker images...
echo ==========================================

REM Web 이미지 빌드
echo Building web image...
docker build -f Dockerfile.web -t monitoring-system-web:%VERSION% .
if errorlevel 1 (
    echo Error building web image
    exit /b 1
)

REM Worker 이미지 빌드
echo Building worker image...
docker build -f Dockerfile.worker -t monitoring-system-worker:%VERSION% .
if errorlevel 1 (
    echo Error building worker image
    exit /b 1
)

echo.
echo ==========================================
echo Saving Docker images...
echo ==========================================

REM Web 이미지 저장 및 압축
echo Saving web image...
docker save monitoring-system-web:%VERSION% -o monitoring-system-web-%VERSION%.tar
powershell -Command "Compress-Archive -Path monitoring-system-web-%VERSION%.tar -DestinationPath monitoring-system-web-%VERSION%.zip -Force"
del monitoring-system-web-%VERSION%.tar

REM Worker 이미지 저장 및 압축
echo Saving worker image...
docker save monitoring-system-worker:%VERSION% -o monitoring-system-worker-%VERSION%.tar
powershell -Command "Compress-Archive -Path monitoring-system-worker-%VERSION%.tar -DestinationPath monitoring-system-worker-%VERSION%.zip -Force"
del monitoring-system-worker-%VERSION%.tar

echo.
echo ==========================================
echo Build completed!
echo ==========================================
echo.
echo Generated files:
echo   - monitoring-system-web-%VERSION%.zip
echo   - monitoring-system-worker-%VERSION%.zip
echo.
```

## 이미지 확인

```powershell
# 빌드된 이미지 목록 확인
docker images | findstr monitoring-system

# 이미지 상세 정보
docker inspect monitoring-system-web:latest

# 이미지 크기 확인
docker images monitoring-system-web:latest
docker images monitoring-system-worker:latest
```

## 문제 해결

### 빌드 캐시 문제

```powershell
# 빌드 캐시 삭제
docker builder prune -a -f

# 캐시 없이 빌드
docker build --no-cache -f Dockerfile.web -t monitoring-system-web:latest .
```

### 디스크 공간 부족

```powershell
# 사용하지 않는 이미지 삭제
docker image prune -a

# 전체 시스템 정리
docker system prune -a --volumes
```

### 빌드 시간 단축

```powershell
# 멀티 스테이지 빌드 최적화 (이미 적용됨)
# 의존성 캐싱 활용 (이미 적용됨)

# 빌드 캐시 활용
docker build -f Dockerfile.web -t monitoring-system-web:latest .
```

## 배포 체크리스트

이미지 생성 전 확인사항:

- [ ] 모든 소스 코드가 최신 상태인지 확인
- [ ] 테스트가 통과했는지 확인
- [ ] 환경 변수 파일(.env) 준비
- [ ] docker-compose.yml 설정 확인
- [ ] 필요한 포트가 열려있는지 확인 (8080, 5432)

## 참고

- Docker 이미지는 환경 변수를 포함하지 않습니다
- 환경 변수는 컨테이너 실행 시 주입됩니다
- `.env` 파일을 사용하면 환경 변수를 쉽게 관리할 수 있습니다
- 프로덕션 환경에서는 Docker Secrets나 외부 설정 관리 도구 사용을 권장합니다


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
