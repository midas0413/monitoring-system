#!/bin/bash

# Monitoring System Docker 시작 스크립트

set -e

echo "=========================================="
echo "Monitoring System Docker 배포"
echo "=========================================="

# .env 파일 확인
if [ ! -f .env ]; then
    echo "⚠️  .env 파일이 없습니다."
    if [ -f env.example ]; then
        echo "📋 env.example을 복사하여 .env 파일을 생성합니다..."
        cp env.example .env
        echo "✅ .env 파일이 생성되었습니다."
        echo "⚠️  .env 파일을 열어서 실제 환경에 맞게 수정하세요!"
        echo ""
        read -p "계속하시겠습니까? (y/n) " -n 1 -r
        echo
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            exit 1
        fi
    else
        echo "❌ env.example 파일도 없습니다. 먼저 환경 변수 파일을 생성하세요."
        exit 1
    fi
fi

# Docker 및 Docker Compose 확인
if ! command -v docker &> /dev/null; then
    echo "❌ Docker가 설치되어 있지 않습니다."
    exit 1
fi

if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo "❌ Docker Compose가 설치되어 있지 않습니다."
    exit 1
fi

# Docker Compose 명령어 확인 (v2 사용 시)
if docker compose version &> /dev/null; then
    DOCKER_COMPOSE="docker compose"
else
    DOCKER_COMPOSE="docker-compose"
fi

echo ""
echo "🔨 Docker 이미지 빌드 중..."
$DOCKER_COMPOSE build

echo ""
echo "🚀 컨테이너 시작 중..."
$DOCKER_COMPOSE up -d

echo ""
echo "⏳ 서비스 시작 대기 중..."
sleep 5

echo ""
echo "📊 컨테이너 상태:"
$DOCKER_COMPOSE ps

echo ""
echo "=========================================="
echo "✅ 배포 완료!"
echo "=========================================="
echo ""
echo "🌐 Web UI: http://localhost:8080"
echo "📊 로그 확인: $DOCKER_COMPOSE logs -f"
echo "🛑 중지: $DOCKER_COMPOSE down"
echo ""
