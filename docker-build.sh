#!/bin/bash
# Docker 이미지 빌드 스크립트

echo "Building monitoring-system Docker images..."

# Web 이미지 빌드
echo "Building web image..."
docker build -f Dockerfile.web -t monitoring-web:latest .

# Worker 이미지 빌드
echo "Building worker image..."
docker build -f Dockerfile.worker -t monitoring-worker:latest .

echo "Build completed!"
echo "To run: docker-compose up -d"
