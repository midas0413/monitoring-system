#!/bin/bash
# Docker 이미지 빌드 및 저장 스크립트 (다른 서버 배포용)

set -e

echo "=========================================="
echo "Building monitoring-system Docker images..."
echo "=========================================="

# 이미지 태그 (버전 관리)
VERSION=${1:-latest}
IMAGE_PREFIX="monitoring-system"

# Web 이미지 빌드
echo ""
echo "Building web image..."
docker build -f Dockerfile.web -t ${IMAGE_PREFIX}-web:${VERSION} .

# Worker 이미지 빌드
echo ""
echo "Building worker image..."
docker build -f Dockerfile.worker -t ${IMAGE_PREFIX}-worker:${VERSION} .

echo ""
echo "=========================================="
echo "Saving Docker images to tar files..."
echo "=========================================="

# 이미지를 tar 파일로 저장
echo ""
echo "Saving web image..."
docker save ${IMAGE_PREFIX}-web:${VERSION} | gzip > ${IMAGE_PREFIX}-web-${VERSION}.tar.gz

echo "Saving worker image..."
docker save ${IMAGE_PREFIX}-worker:${VERSION} | gzip > ${IMAGE_PREFIX}-worker-${VERSION}.tar.gz

echo ""
echo "=========================================="
echo "Build and save completed!"
echo "=========================================="
echo ""
echo "Generated files:"
echo "  - ${IMAGE_PREFIX}-web-${VERSION}.tar.gz"
echo "  - ${IMAGE_PREFIX}-worker-${VERSION}.tar.gz"
echo ""
echo "To deploy to another server:"
echo "  1. Copy the tar.gz files to the target server"
echo "  2. Run: docker load -i ${IMAGE_PREFIX}-web-${VERSION}.tar.gz"
echo "  3. Run: docker load -i ${IMAGE_PREFIX}-worker-${VERSION}.tar.gz"
echo "  4. Update docker-compose.yml to use the loaded images"
echo ""
