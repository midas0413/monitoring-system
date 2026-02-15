@echo off
REM 다른 서버 배포용 단일 ZIP 생성 (이미지 + docker-compose + .env.example + 로드 스크립트)
REM 사용: build-deploy-zip.bat [버전]
REM 결과: monitoring-system-deploy-{version}.zip

setlocal enabledelayedexpansion

set VERSION=%1
if "%VERSION%"=="" set VERSION=latest

set IMAGE_PREFIX=monitoring-system
set DEPLOY_DIR=%IMAGE_PREFIX%-deploy-%VERSION%
set DEPLOY_ZIP=%DEPLOY_DIR%.zip

echo ==========================================
echo 1. Docker 이미지 빌드 및 ZIP 저장
echo ==========================================
call docker-build-and-save.bat %VERSION%
if errorlevel 1 exit /b 1

echo.
echo ==========================================
echo 2. 배포 패키지 폴더 생성
echo ==========================================

if exist "%DEPLOY_DIR%" rmdir /s /q "%DEPLOY_DIR%"
mkdir "%DEPLOY_DIR%"

copy "%IMAGE_PREFIX%-web-%VERSION%.zip"    "%DEPLOY_DIR%\"
copy "%IMAGE_PREFIX%-worker-%VERSION%.zip" "%DEPLOY_DIR%\"
copy "docker-compose.deploy.yml"           "%DEPLOY_DIR%\"
copy ".env.example"                         "%DEPLOY_DIR%\"
copy "docker-deploy.bat"                    "%DEPLOY_DIR%\"

echo [배포 안내 - 다른 서버에서 실행 순서] > "%DEPLOY_DIR%\배포_안내.txt"
echo. >> "%DEPLOY_DIR%\배포_안내.txt"
echo 1. 이 폴더의 압축을 풀었으면, .env 설정: >> "%DEPLOY_DIR%\배포_안내.txt"
echo    copy .env.example .env >> "%DEPLOY_DIR%\배포_안내.txt"
echo    notepad .env   (DB, Aligo 등 실제 값으로 수정) >> "%DEPLOY_DIR%\배포_안내.txt"
echo. >> "%DEPLOY_DIR%\배포_안내.txt"
echo 2. Docker 이미지 로드: >> "%DEPLOY_DIR%\배포_안내.txt"
echo    docker-deploy.bat %VERSION% >> "%DEPLOY_DIR%\배포_안내.txt"
echo. >> "%DEPLOY_DIR%\배포_안내.txt"
echo 3. 서비스 실행: >> "%DEPLOY_DIR%\배포_안내.txt"
echo    docker-compose -f docker-compose.deploy.yml up -d >> "%DEPLOY_DIR%\배포_안내.txt"
echo. >> "%DEPLOY_DIR%\배포_안내.txt"
echo 자세한 내용: DEPLOY.md, ENV_SETUP.md >> "%DEPLOY_DIR%\배포_안내.txt"

echo.
echo ==========================================
echo 3. 배포용 ZIP 생성
echo ==========================================

if exist "%DEPLOY_ZIP%" del "%DEPLOY_ZIP%"
powershell -Command "Compress-Archive -Path '%DEPLOY_DIR%' -DestinationPath '%DEPLOY_ZIP%' -Force"
rmdir /s /q "%DEPLOY_DIR%"

echo.
echo ==========================================
echo 완료
echo ==========================================
echo.
echo 생성 파일: %DEPLOY_ZIP%
echo.
echo 다른 서버에 복사한 뒤:
echo   1. 압축 해제
echo   2. .env.example 을 .env 로 복사 후 수정
echo   3. docker-deploy.bat %VERSION%  (이미지 로드)
echo   4. docker-compose -f docker-compose.deploy.yml up -d
echo.
