# Docker 이미지 빌드 완료

## 생성된 파일

다음 Docker 이미지 파일들이 생성되었습니다:

### Windows 환경
- `monitoring-system-web-latest.zip` - Web 애플리케이션 이미지 (압축)
- `monitoring-system-worker-latest.zip` - Worker 애플리케이션 이미지 (압축)

### Linux/Mac 환경 (스크립트 실행 시)
- `monitoring-system-web-latest.tar.gz` - Web 애플리케이션 이미지 (압축)
- `monitoring-system-worker-latest.tar.gz` - Worker 애플리케이션 이미지 (압축)

## 이미지 정보

- **Web 이미지**: `monitoring-system-web:latest`
- **Worker 이미지**: `monitoring-system-worker:latest`

## 배포 방법

### 1. 파일 전송
압축 파일을 대상 서버로 전송합니다.

### 2. 이미지 로드
대상 서버에서 압축을 해제하고 Docker 이미지를 로드합니다.

자세한 내용은 [DEPLOY.md](DEPLOY.md) 또는 [QUICK_START.md](QUICK_START.md)를 참조하세요.

## 다음 단계

1. 압축 파일을 대상 서버로 전송
2. 대상 서버에서 이미지 로드
3. `docker-compose.deploy.yml` 사용하여 서비스 실행
4. 환경 변수 설정 (`.env` 파일 또는 환경 변수)
5. 데이터베이스 초기화 (선택사항)
