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
