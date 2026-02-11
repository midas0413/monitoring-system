# 환경 변수 설정 가이드

## 파일 위치

### 대상 서버에서의 위치

`.env` 파일은 **프로젝트 루트 디렉토리**에 위치해야 합니다:

```
monitoring-system/
├── docker-compose.yml
├── .env                    ← 여기에 위치
├── .env.example
├── Dockerfile.web
├── Dockerfile.worker
└── ...
```

**중요**: `docker-compose.yml` 파일과 **같은 디렉토리**에 있어야 합니다!

## 설정 방법

### 1. .env.example 파일 복사

```powershell
# Windows PowerShell
Copy-Item .env.example .env

# 또는 CMD
copy .env.example .env
```

### 2. .env 파일 수정

`.env` 파일을 열어서 실제 값으로 수정합니다:

```env
# 데이터베이스 설정
SPRING_DATASOURCE_URL=jdbc:postgresql://172.22.1.113:5432/monitoring
SPRING_DATASOURCE_USERNAME=monitoring
SPRING_DATASOURCE_PASSWORD=your-actual-password

# Aligo API 설정
ALIGO_API_KEY=your-actual-api-key
ALIGO_USER_ID=your-actual-user-id
ALIGO_SENDER=02-736-3500
ALIGO_SENDER_KEY=your-actual-sender-key
ALIGO_TEMPLATE_CODE=UC_2669
ALIGO_TEST_MODE=N

# 메일 설정
SPRING_MAIL_USERNAME=your-email@gmail.com
SPRING_MAIL_PASSWORD=your-actual-password
```

### 3. Docker Compose 실행

`.env` 파일이 자동으로 읽혀집니다:

```powershell
docker-compose up -d
```

## 파일 구조 예시

### 개발/빌드 서버
```
D:\JavaWorkspace\monitoring-system\
├── docker-compose.yml
├── .env.example          ← 템플릿 (Git에 포함)
├── .env                  ← 실제 설정 (Git 제외, 로컬에만 존재)
├── Dockerfile.web
├── Dockerfile.worker
└── ...
```

### 배포 대상 서버
```
C:\monitoring-system\     (또는 원하는 경로)
├── docker-compose.yml
├── .env                  ← 여기에 복사하여 배치
├── Dockerfile.web
├── Dockerfile.worker
└── ...
```

## 배포 시 주의사항

### 1. .env 파일은 Git에 커밋하지 않기

`.env` 파일은 민감한 정보(비밀번호, API 키 등)를 포함하므로 **절대 Git에 커밋하지 마세요!**

`.gitignore`에 이미 `.env`가 포함되어 있습니다.

### 2. 배포 시 .env 파일 전송 방법

#### 방법 1: 수동 복사 (권장)
- `.env` 파일을 안전한 방법으로 대상 서버에 복사
- SFTP, SCP, 또는 보안 USB 사용

#### 방법 2: 암호화된 파일로 전송
- `.env` 파일을 암호화하여 전송
- 대상 서버에서 복호화

#### 방법 3: 환경 변수 직접 설정
- Docker Compose 실행 시 환경 변수를 직접 설정
- 예: `$env:ALIGO_API_KEY="your-key"; docker-compose up -d`

### 3. 파일 권한 설정 (Linux 서버인 경우)

```bash
chmod 600 .env  # 소유자만 읽기/쓰기 가능
```

## 환경 변수 목록

### 필수 설정

| 변수명 | 설명 | 예시 |
|--------|------|------|
| `SPRING_DATASOURCE_URL` | 데이터베이스 연결 URL | `jdbc:postgresql://172.22.1.113:5432/monitoring` |
| `SPRING_DATASOURCE_USERNAME` | 데이터베이스 사용자명 | `monitoring` |
| `SPRING_DATASOURCE_PASSWORD` | 데이터베이스 비밀번호 | `your-password` |
| `ALIGO_API_KEY` | Aligo API 키 | `ou73by0iwvi33psmwrgvsjgvxtsc8t7f` |
| `ALIGO_USER_ID` | Aligo 사용자 ID | `midas09` |
| `ALIGO_SENDER` | 발신번호 | `02-736-3500` |
| `ALIGO_SENDER_KEY` | 알림톡 발신프로필 키 | `fab6a2937b472d39f03bb3ca2873bd331ce3a7af` |
| `ALIGO_TEMPLATE_CODE` | 알림톡 템플릿 코드 | `UC_2669` |

### 선택 설정

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `ALIGO_TEST_MODE` | 테스트 모드 (Y/N) | `N` |
| `WORKER_ID` | Worker ID | 자동 생성 |
| `WORKER_CLAIM_LIMIT` | Worker 처리 한도 | `10` |
| `WORKER_LOCK_SECONDS` | 룰 잠금 시간 | `30` |
| `SPRING_MAIL_HOST` | 메일 서버 | `smtp.gmail.com` |
| `SPRING_MAIL_PORT` | 메일 포트 | `587` |
| `APP_SYSTEM_NAME` | 시스템 이름 | `Monitoring Portal` |

## 확인 방법

### 환경 변수가 제대로 로드되었는지 확인

```powershell
# 컨테이너 내부 환경 변수 확인
docker-compose exec web env | findstr ALIGO
docker-compose exec worker env | findstr ALIGO
```

### 로그에서 확인

```powershell
# Web 로그 확인
docker-compose logs web | findstr "Aligo"

# Worker 로그 확인
docker-compose logs worker | findstr "Aligo"
```

## 문제 해결

### .env 파일이 읽히지 않는 경우

1. **파일 위치 확인**: `docker-compose.yml`과 같은 디렉토리에 있는지 확인
2. **파일 이름 확인**: `.env` (점으로 시작, 확장자 없음)
3. **파일 인코딩 확인**: UTF-8 (BOM 없음)
4. **Docker Compose 재시작**: `docker-compose down && docker-compose up -d`

### 환경 변수가 적용되지 않는 경우

1. **docker-compose.yml 확인**: `env_file: - .env` 항목이 있는지 확인
2. **컨테이너 재시작**: `docker-compose restart web worker`
3. **환경 변수 직접 확인**: `docker-compose exec web printenv ALIGO_API_KEY`

## 보안 권장사항

1. ✅ `.env` 파일은 Git에 커밋하지 않기
2. ✅ 파일 권한을 제한 (소유자만 읽기/쓰기)
3. ✅ 정기적으로 비밀번호 및 API 키 변경
4. ✅ 프로덕션 환경에서는 더 강력한 비밀번호 사용
5. ✅ `.env` 파일 백업 시 암호화
