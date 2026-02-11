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

if not exist "%IMAGE_PREFIX%-web-%VERSION%.tar.gz" (
    echo Error: %IMAGE_PREFIX%-web-%VERSION%.tar.gz not found!
    exit /b 1
)

if not exist "%IMAGE_PREFIX%-worker-%VERSION%.tar.gz" (
    echo Error: %IMAGE_PREFIX%-worker-%VERSION%.tar.gz not found!
    exit /b 1
)

echo Extracting web image...
powershell -Command "Expand-Archive -Path %IMAGE_PREFIX%-web-%VERSION%.zip -DestinationPath . -Force"
echo Loading web image...
docker load -i %IMAGE_PREFIX%-web-%VERSION%.tar
del %IMAGE_PREFIX%-web-%VERSION%.tar

if errorlevel 1 (
    echo Error loading web image
    exit /b 1
)

echo Extracting worker image...
powershell -Command "Expand-Archive -Path %IMAGE_PREFIX%-worker-%VERSION%.zip -DestinationPath . -Force"
echo Loading worker image...
docker load -i %IMAGE_PREFIX%-worker-%VERSION%.tar
del %IMAGE_PREFIX%-worker-%VERSION%.tar

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
echo To run with docker-compose, update docker-compose.yml:
echo   web:
echo     image: %IMAGE_PREFIX%-web:%VERSION%
echo   worker:
echo     image: %IMAGE_PREFIX%-worker:%VERSION%
echo.
