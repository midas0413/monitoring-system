# Docker 배포 가이드

이 문서는 Monitoring System을 Docker로 배포하는 방법을 설명합니다.

## 사전 요구사항

- Docker 20.10 이상
- Docker Compose 2.0 이상
- 최소 4GB RAM (권장: 8GB)
- 최소 10GB 디스크 공간

## 빠른 시작

### 1. 환경 변수 파일 생성

```bash
cp .env.example .env
```

### 2. 환경 변수 설정

`.env` 파일을 열어서 실제 환경에 맞게 수정합니다:

```bash
# 필수 설정
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/monitoring
SPRING_DATASOURCE_USERNAME=monitoring
SPRING_DATASOURCE_PASSWORD=your-secure-password

# Aligo API 설정 (SMS/카카오 알림톡)
ALIGO_API_KEY=your-api-key
ALIGO_USER_ID=your-user-id
ALIGO_SENDER=02-0000-0000
ALIGO_SENDER_KEY=your-sender-key
```

### 3. Docker 이미지 빌드 및 실행

```bash
# 이미지 빌드 및 컨테이너 시작
docker-compose up -d

# 로그 확인
docker-compose logs -f

# 특정 서비스 로그만 확인
docker-compose logs -f web
docker-compose logs -f worker
```

### 4. 애플리케이션 접속

- Web UI: http://localhost:8080
- PostgreSQL: localhost:5432

## 상세 설정

### 환경 변수 설명

#### 데이터베이스 설정

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/monitoring
SPRING_DATASOURCE_USERNAME=monitoring
SPRING_DATASOURCE_PASSWORD=monitoring
```

**외부 데이터베이스 사용 시:**
```env
SPRING_DATASOURCE_URL=jdbc:postgresql://your-db-host:5432/monitoring
SPRING_DATASOURCE_USERNAME=your-username
SPRING_DATASOURCE_PASSWORD=your-password
```

#### Aligo API 설정

SMS 및 카카오 알림톡 발송을 위한 설정입니다.

1. [알리고 사이트](https://smartsms.aligo.in)에서 계정 생성
2. API 키 발급
3. 발신번호 등록
4. 카카오 알림톡 사용 시: 카카오 채널 발신프로필 키 발급

```env
ALIGO_API_KEY=your-api-key
ALIGO_USER_ID=your-user-id
ALIGO_SENDER=02-0000-0000
ALIGO_SENDER_KEY=your-sender-key
ALIGO_TEMPLATE_CODE=UC_2669
ALIGO_TEST_MODE=N
```

#### 이메일 설정 (SMTP)

```env
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=your-email@gmail.com
SPRING_MAIL_PASSWORD=your-app-password
```

**Gmail 사용 시:**
- 2단계 인증 활성화 필요
- 앱 비밀번호 생성 필요 (일반 비밀번호 아님)
- [Google 계정 관리](https://myaccount.google.com/apppasswords)에서 생성

#### Worker 설정

```env
WORKER_ID=worker-1
WORKER_CLAIM_LIMIT=10
WORKER_LOCK_SECONDS=30
WORKER_LOOP_SLEEP_MS=1000
```

### 프로덕션 배포

프로덕션 환경에서는 `docker-compose.prod.yml`을 함께 사용합니다:

```bash
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

프로덕션 설정의 특징:
- `restart: always` - 컨테이너 자동 재시작
- 리소스 제한 설정
- 더 엄격한 로깅 레벨 (WARN)

### Worker 확장

여러 Worker를 실행하여 부하 분산:

```bash
# 3개의 Worker 실행
docker-compose up -d --scale worker=3
```

### 데이터베이스 백업

```bash
# 백업
docker-compose exec postgres pg_dump -U monitoring monitoring > backup.sql

# 복원
docker-compose exec -T postgres psql -U monitoring monitoring < backup.sql
```

### 로그 관리

```bash
# 모든 로그 확인
docker-compose logs

# 실시간 로그 확인
docker-compose logs -f

# 특정 서비스 로그만 확인
docker-compose logs -f web
docker-compose logs -f worker
docker-compose logs -f postgres

# 최근 100줄만 확인
docker-compose logs --tail=100

# 특정 시간 이후 로그
docker-compose logs --since 2024-01-01T00:00:00
```

### 컨테이너 관리

```bash
# 컨테이너 상태 확인
docker-compose ps

# 컨테이너 중지
docker-compose stop

# 컨테이너 시작
docker-compose start

# 컨테이너 재시작
docker-compose restart

# 컨테이너 중지 및 제거
docker-compose down

# 컨테이너 중지, 제거 및 볼륨 삭제 (주의!)
docker-compose down -v
```

### 이미지 업데이트

```bash
# 최신 코드로 이미지 재빌드
docker-compose build

# 재빌드 후 재시작
docker-compose up -d --build

# 특정 서비스만 재빌드
docker-compose build web
docker-compose up -d web
```

### 문제 해결

#### 포트 충돌

다른 애플리케이션이 8080 또는 5432 포트를 사용 중인 경우:

```yaml
# docker-compose.yml 수정
services:
  web:
    ports:
      - "8081:8080"  # 외부 포트 변경
  postgres:
    ports:
      - "5433:5432"  # 외부 포트 변경
```

#### 외부에서 접속 불가

외부에서 8080 포트로 접속할 수 없는 경우:

1. **포트 바인딩 확인**
   ```yaml
   # docker-compose.yml에서 명시적으로 모든 인터페이스에 바인딩
   ports:
     - "0.0.0.0:8080:8080"  # 모든 네트워크 인터페이스에서 접속 가능
   ```

2. **Windows 방화벽 설정**
   - Windows 방화벽에서 8080 포트 인바운드 규칙 추가
   - PowerShell (관리자 권한):
     ```powershell
     New-NetFirewallRule -DisplayName "Docker Web Port" -Direction Inbound -LocalPort 8080 -Protocol TCP -Action Allow
     ```

3. **Docker Desktop 네트워크 설정**
   - Docker Desktop > Settings > Resources > Network
   - WSL 2 integration 활성화 (WSL 사용 시)

4. **포트 사용 확인**
   ```powershell
   # 8080 포트 사용 중인 프로세스 확인
   netstat -ano | findstr :8080
   ```

5. **컨테이너 재시작**
   ```powershell
   docker-compose down
   docker-compose up -d
   ```

#### 데이터베이스 연결 오류

1. PostgreSQL 컨테이너가 실행 중인지 확인:
   ```bash
   docker-compose ps postgres
   ```

2. 데이터베이스 로그 확인:
   ```bash
   docker-compose logs postgres
   ```

3. 환경 변수 확인:
   ```bash
   docker-compose exec web env | grep SPRING_DATASOURCE
   ```

#### 메모리 부족

컨테이너에 메모리 제한을 설정:

```yaml
# docker-compose.yml에 추가
services:
  web:
    deploy:
      resources:
        limits:
          memory: 1G
```

#### 디스크 공간 부족

사용하지 않는 이미지 및 컨테이너 정리:

```bash
# 사용하지 않는 이미지 삭제
docker image prune -a

# 사용하지 않는 컨테이너, 네트워크, 볼륨 삭제
docker system prune -a --volumes
```

## 보안 권장사항

1. **환경 변수 보호**
   - `.env` 파일을 Git에 커밋하지 마세요
   - 프로덕션 환경에서는 Docker Secrets 또는 외부 설정 관리 도구 사용

2. **데이터베이스 비밀번호**
   - 강력한 비밀번호 사용
   - 정기적으로 비밀번호 변경

3. **네트워크 격리**
   - 프로덕션 환경에서는 외부에서 직접 접근 불가능하도록 방화벽 설정
   - 리버스 프록시 (Nginx, Traefik) 사용 권장

4. **로그 관리**
   - 민감한 정보가 로그에 출력되지 않도록 주의
   - 로그 로테이션 설정

## 모니터링

### Health Check

Web 서비스는 자동으로 Health Check를 수행합니다:

```bash
# Health Check 상태 확인
docker-compose ps
```

### 리소스 사용량 확인

```bash
# 컨테이너 리소스 사용량
docker stats

# 특정 컨테이너만 확인
docker stats monitoring-web monitoring-worker
```

## 추가 리소스

- [Docker 공식 문서](https://docs.docker.com/)
- [Docker Compose 공식 문서](https://docs.docker.com/compose/)
- [PostgreSQL 공식 문서](https://www.postgresql.org/docs/)
