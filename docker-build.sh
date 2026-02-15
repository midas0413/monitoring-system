#!/bin/bash
# Docker 배포 이미지 빌드
# .env + docker-compose.deploy.yml 사용: docker-compose -f docker-compose.deploy.yml up -d

IMAGE_PREFIX=monitoring-system
VERSION=${1:-latest}

set -e
echo "Building $IMAGE_PREFIX Docker images..."

echo "Building web image..."
docker build -f Dockerfile.web -t "$IMAGE_PREFIX-web:$VERSION" .

echo "Building worker image..."
docker build -f Dockerfile.worker -t "$IMAGE_PREFIX-worker:$VERSION" .

echo ""
echo "Build completed! Images: $IMAGE_PREFIX-web:$VERSION, $IMAGE_PREFIX-worker:$VERSION"
echo "Run with .env: docker-compose -f docker-compose.deploy.yml up -d"
