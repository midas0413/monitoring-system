@echo off
REM Docker Web 이미지 빌드 및 tar 파일 저장 스크립트 (Windows)

setlocal enabledelayedexpansion

set VERSION=%1
if "%VERSION%"=="" set VERSION=latest

set IMAGE_PREFIX=monitoring-system
set IMAGE_NAME=%IMAGE_PREFIX%-web
set TAR_FILE=%IMAGE_NAME%-%VERSION%.tar

echo ==========================================
echo Building monitoring-web Docker image...
echo ==========================================
echo.

echo Building web image...
docker build -f Dockerfile.web -t %IMAGE_NAME%:%VERSION% .

if errorlevel 1 (
    echo Error building web image
    exit /b 1
)

echo.
echo ==========================================
echo Saving Docker image to tar file...
echo ==========================================
echo.

echo Saving web image to %TAR_FILE%...
docker save %IMAGE_NAME%:%VERSION% -o %TAR_FILE%

if errorlevel 1 (
    echo Error saving web image
    exit /b 1
)

if exist %TAR_FILE% (
    echo.
    echo ==========================================
    echo Build and save completed!
    echo ==========================================
    echo.
    echo Generated file:
    echo   - %TAR_FILE%
    echo.
    echo File size:
    for %%A in (%TAR_FILE%) do echo   - %%~zA bytes (%%~zA / 1024 / 1024 MB)
    echo.
    echo To load the image on another server:
    echo   docker load -i %TAR_FILE%
    echo.
) else (
    echo Error: Tar file was not created: %TAR_FILE%
    exit /b 1
)
