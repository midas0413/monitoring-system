@echo off
REM 다른 서버에 배포하기 위한 Docker 이미지 로드 스크립트 - Windows

setlocal enabledelayedexpansion

set VERSION=%1
if "%VERSION%"=="" set VERSION=latest

set IMAGE_PREFIX=monitoring-system

echo ==========================================
echo Loading Docker images...
echo ==========================================
echo.

REM Windows: .zip / Linux 전송분: .tar.gz
set WEB_FILE=
set WORKER_FILE=
if exist "%IMAGE_PREFIX%-web-%VERSION%.zip"    set WEB_FILE=zip
if exist "%IMAGE_PREFIX%-web-%VERSION%.tar.gz" set WEB_FILE=targz
if exist "%IMAGE_PREFIX%-worker-%VERSION%.zip"    set WORKER_FILE=zip
if exist "%IMAGE_PREFIX%-worker-%VERSION%.tar.gz" set WORKER_FILE=targz

if "%WEB_FILE%"=="" (
    echo Error: %IMAGE_PREFIX%-web-%VERSION%.zip or .tar.gz not found!
    exit /b 1
)
if "%WORKER_FILE%"=="" (
    echo Error: %IMAGE_PREFIX%-worker-%VERSION%.zip or .tar.gz not found!
    exit /b 1
)

if "%WEB_FILE%"=="zip" (
    echo Extracting web image...
    powershell -Command "Expand-Archive -Path '%IMAGE_PREFIX%-web-%VERSION%.zip' -DestinationPath . -Force"
    echo Loading web image...
    docker load -i %IMAGE_PREFIX%-web-%VERSION%.tar
    del %IMAGE_PREFIX%-web-%VERSION%.tar
) else (
    echo Loading web image from tar.gz...
    powershell -Command "& { tar -xzf '%IMAGE_PREFIX%-web-%VERSION%.tar.gz' -C . }"
    docker load -i %IMAGE_PREFIX%-web-%VERSION%.tar
    del %IMAGE_PREFIX%-web-%VERSION%.tar
)
if errorlevel 1 (
    echo Error loading web image
    exit /b 1
)

if "%WORKER_FILE%"=="zip" (
    echo Extracting worker image...
    powershell -Command "Expand-Archive -Path '%IMAGE_PREFIX%-worker-%VERSION%.zip' -DestinationPath . -Force"
    echo Loading worker image...
    docker load -i %IMAGE_PREFIX%-worker-%VERSION%.tar
    del %IMAGE_PREFIX%-worker-%VERSION%.tar
) else (
    echo Loading worker image from tar.gz...
    powershell -Command "& { tar -xzf '%IMAGE_PREFIX%-worker-%VERSION%.tar.gz' -C . }"
    docker load -i %IMAGE_PREFIX%-worker-%VERSION%.tar
    del %IMAGE_PREFIX%-worker-%VERSION%.tar
)
if errorlevel 1 (
    echo Error loading worker image
    exit /b 1
)

echo.
echo ==========================================
echo Images loaded successfully!
echo ==========================================
echo.
echo Available images:
docker images | findstr %IMAGE_PREFIX%
echo.
echo Run: docker-compose -f docker-compose.deploy.yml up -d
echo (Ensure .env is in the same folder; copy from .env.example if needed.)
echo.
