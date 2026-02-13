@echo off
REM Monitoring System Docker 시작 스크립트 (Windows)

echo ==========================================
echo Monitoring System Docker 배포
echo ==========================================
echo.

REM .env 파일 확인
if not exist .env (
    echo [경고] .env 파일이 없습니다.
    if exist env.example (
        echo [정보] env.example을 복사하여 .env 파일을 생성합니다...
        copy env.example .env
        echo [완료] .env 파일이 생성되었습니다.
        echo [경고] .env 파일을 열어서 실제 환경에 맞게 수정하세요!
        echo.
        pause
    ) else (
        echo [오류] env.example 파일도 없습니다. 먼저 환경 변수 파일을 생성하세요.
        pause
        exit /b 1
    )
)

REM Docker 확인
where docker >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo [오류] Docker가 설치되어 있지 않습니다.
    pause
    exit /b 1
)

REM Docker Compose 확인
docker compose version >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    set DOCKER_COMPOSE=docker compose
) else (
    where docker-compose >nul 2>nul
    if %ERRORLEVEL% NEQ 0 (
        echo [오류] Docker Compose가 설치되어 있지 않습니다.
        pause
        exit /b 1
    )
    set DOCKER_COMPOSE=docker-compose
)

echo.
echo [빌드] Docker 이미지 빌드 중...
%DOCKER_COMPOSE% build
if %ERRORLEVEL% NEQ 0 (
    echo [오류] 빌드 실패
    pause
    exit /b 1
)

echo.
echo [시작] 컨테이너 시작 중...
%DOCKER_COMPOSE% up -d
if %ERRORLEVEL% NEQ 0 (
    echo [오류] 컨테이너 시작 실패
    pause
    exit /b 1
)

echo.
echo [대기] 서비스 시작 대기 중...
timeout /t 5 /nobreak >nul

echo.
echo [상태] 컨테이너 상태:
%DOCKER_COMPOSE% ps

echo.
echo ==========================================
echo [완료] 배포 완료!
echo ==========================================
echo.
echo [접속] Web UI: http://localhost:8080
echo [로그] 로그 확인: %DOCKER_COMPOSE% logs -f
echo [중지] 중지: %DOCKER_COMPOSE% down
echo.
pause
