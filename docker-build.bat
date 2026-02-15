@echo off
REM Docker 배포 이미지 빌드 (Windows)
REM .env + docker-compose.deploy.yml 사용 시: docker-compose -f docker-compose.deploy.yml up -d

set IMAGE_PREFIX=monitoring-system
set VERSION=latest

echo Building %IMAGE_PREFIX% Docker images...

echo Building web image...
docker build -f Dockerfile.web -t %IMAGE_PREFIX%-web:%VERSION% .
if errorlevel 1 exit /b 1

echo Building worker image...
docker build -f Dockerfile.worker -t %IMAGE_PREFIX%-worker:%VERSION% .
if errorlevel 1 exit /b 1

echo.
echo Build completed! Images: %IMAGE_PREFIX%-web:%VERSION%, %IMAGE_PREFIX%-worker:%VERSION%
echo Run with .env: docker-compose -f docker-compose.deploy.yml up -d
