@echo off
REM Docker 이미지 빌드 및 저장 스크립트 (다른 서버 배포용) - Windows

setlocal enabledelayedexpansion

set VERSION=%1
if "%VERSION%"=="" set VERSION=latest

set IMAGE_PREFIX=monitoring-system

echo ==========================================
echo Building monitoring-system Docker images...
echo ==========================================
echo.

echo Building web image...
docker build -f Dockerfile.web -t %IMAGE_PREFIX%-web:%VERSION% .

if errorlevel 1 (
    echo Error building web image
    exit /b 1
)

echo.
echo Building worker image...
docker build -f Dockerfile.worker -t %IMAGE_PREFIX%-worker:%VERSION% .

if errorlevel 1 (
    echo Error building worker image
    exit /b 1
)

echo.
echo ==========================================
echo Saving Docker images to tar files...
echo ==========================================
echo.

echo Saving web image...
docker save %IMAGE_PREFIX%-web:%VERSION% -o %IMAGE_PREFIX%-web-%VERSION%.tar

if errorlevel 1 (
    echo Error saving web image
    exit /b 1
)

echo Compressing web image...
if exist %IMAGE_PREFIX%-web-%VERSION%.tar (
    powershell -ExecutionPolicy Bypass -Command "Compress-Archive -Path '%IMAGE_PREFIX%-web-%VERSION%.tar' -DestinationPath '%IMAGE_PREFIX%-web-%VERSION%.zip' -Force"
    if errorlevel 1 (
        echo Error compressing web image
        exit /b 1
    )
    if exist %IMAGE_PREFIX%-web-%VERSION%.zip (
        echo Web image zip file created successfully
        del %IMAGE_PREFIX%-web-%VERSION%.tar
    ) else (
        echo Error: Zip file was not created
        exit /b 1
    )
) else (
    echo Error: Tar file does not exist: %IMAGE_PREFIX%-web-%VERSION%.tar
    exit /b 1
)

echo Saving worker image...
docker save %IMAGE_PREFIX%-worker:%VERSION% -o %IMAGE_PREFIX%-worker-%VERSION%.tar

if errorlevel 1 (
    echo Error saving worker image
    exit /b 1
)

echo Compressing worker image...
if exist %IMAGE_PREFIX%-worker-%VERSION%.tar (
    powershell -ExecutionPolicy Bypass -Command "Compress-Archive -Path '%IMAGE_PREFIX%-worker-%VERSION%.tar' -DestinationPath '%IMAGE_PREFIX%-worker-%VERSION%.zip' -Force"
    if errorlevel 1 (
        echo Error compressing worker image
        exit /b 1
    )
    if exist %IMAGE_PREFIX%-worker-%VERSION%.zip (
        echo Worker image zip file created successfully
        del %IMAGE_PREFIX%-worker-%VERSION%.tar
    ) else (
        echo Error: Zip file was not created
        exit /b 1
    )
) else (
    echo Error: Tar file does not exist: %IMAGE_PREFIX%-worker-%VERSION%.tar
    exit /b 1
)

echo.
echo ==========================================
echo Build and save completed!
echo ==========================================
echo.
echo Generated files:
echo   - %IMAGE_PREFIX%-web-%VERSION%.zip
echo   - %IMAGE_PREFIX%-worker-%VERSION%.zip
echo.
echo To deploy to another server:
echo   1. Copy the zip files to the target server
echo   2. Extract: powershell -Command "Expand-Archive -Path %IMAGE_PREFIX%-web-%VERSION%.zip -DestinationPath . -Force"
echo   3. Extract: powershell -Command "Expand-Archive -Path %IMAGE_PREFIX%-worker-%VERSION%.zip -DestinationPath . -Force"
echo   4. Load: docker load -i %IMAGE_PREFIX%-web-%VERSION%.tar
echo   5. Load: docker load -i %IMAGE_PREFIX%-worker-%VERSION%.tar
echo   6. Update docker-compose.yml to use the loaded images
echo.
