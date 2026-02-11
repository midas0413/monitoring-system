#!/bin/bash
# 다른 서버에 배포하기 위한 Docker 이미지 로드 스크립트

set -e

VERSION=${1:-latest}
IMAGE_PREFIX="monitoring-system"

echo "=========================================="
echo "Loading Docker images..."
echo "=========================================="

if [ ! -f "${IMAGE_PREFIX}-web-${VERSION}.tar.gz" ]; then
    echo "Error: ${IMAGE_PREFIX}-web-${VERSION}.tar.gz not found!"
    exit 1
fi

if [ ! -f "${IMAGE_PREFIX}-worker-${VERSION}.tar.gz" ]; then
    echo "Error: ${IMAGE_PREFIX}-worker-${VERSION}.tar.gz not found!"
    exit 1
fi

echo ""
echo "Loading web image..."
gunzip -c ${IMAGE_PREFIX}-web-${VERSION}.tar.gz | docker load

echo "Loading worker image..."
gunzip -c ${IMAGE_PREFIX}-worker-${VERSION}.tar.gz | docker load

echo ""
echo "=========================================="
echo "Images loaded successfully!"
echo "=========================================="
echo ""
echo "Available images:"
docker images | grep ${IMAGE_PREFIX}
echo ""
echo "To run with docker-compose, update docker-compose.yml:"
echo "  web:"
echo "    image: ${IMAGE_PREFIX}-web:${VERSION}"
echo "  worker:"
echo "    image: ${IMAGE_PREFIX}-worker:${VERSION}"
echo ""
