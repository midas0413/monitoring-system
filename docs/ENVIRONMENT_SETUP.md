# 테스트 / 운영 환경 분리 가이드

테스트는 **localhost Docker PostgreSQL**, 운영은 **실제 운영 DB**를 쓰도록 설정하는 방법입니다.

---

## 1. 요약

| 구분 | DB | 설정 방법 |
|------|-----|-----------|
| **테스트** | Docker PostgreSQL (localhost) | `.env` 없이 docker-compose 기본값 사용 또는 `.env.local` |
| **운영** | 실제 운영 DB 서버 | `.env` 또는 시스템 환경 변수에 운영 DB URL 설정 |

Spring은 `SPRING_DATASOURCE_URL` 등 **환경 변수**를 우선 사용하므로, 같은 코드로 환경만 바꿔서 사용할 수 있습니다.

---

## 2. 테스트 환경 (로컬 Docker PostgreSQL)

### 2-1. docker-compose로 전체 실행 (권장)

Web·Worker가 Docker 안에서 실행되므로 DB 호스트는 **서비스 이름 `postgres`** 입니다.

- **`.env` 파일을 두지 않거나**, 아래만 넣은 `.env` 사용:

```env
# 테스트용 - Docker 내부 postgres 사용 (기본값과 동일하므로 생략 가능)
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/monitoring
SPRING_DATASOURCE_USERNAME=monitoring
SPRING_DATASOURCE_PASSWORD=monitoring
```

- 실행:

```bash
docker-compose up -d
```

- `docker-compose.yml` 기본값이 이미 `SPRING_DATASOURCE_URL:-jdbc:postgresql://postgres:5432/monitoring` 이므로, **.env에 DB 항목을 아예 안 넣어도** 테스트용으로는 Docker Postgres에 연결됩니다.

### 2-2. Postgres만 Docker로 띄우고, Web/Worker는 로컬에서 실행

Postgres만 Docker로 띄우고, 브라우저/IDE에서 Web을 localhost로 띄우는 경우:

- Postgres만 기동:

```bash
docker-compose up -d postgres
```

- Web/Worker는 **로컬**에서 실행하므로 DB 호스트는 **localhost**:

```bash
# Windows (PowerShell)
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/monitoring"
$env:SPRING_DATASOURCE_USERNAME="monitoring"
$env:SPRING_DATASOURCE_PASSWORD="monitoring"
.\gradlew :monitoring-web:bootRun

# 또는 .env.local 파일을 만들어 두고 (Git 제외 권장)
# SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/monitoring
# SPRING_DATASOURCE_USERNAME=monitoring
# SPRING_DATASOURCE_PASSWORD=monitoring
```

---

## 3. 운영 환경 (실제 운영 DB)

운영 서버에서는 **환경 변수**로만 운영 DB를 가리키면 됩니다.

### 3-1. .env 파일로 설정 (docker-compose 사용 시)

운영 서버의 프로젝트 디렉터리에 `.env` 파일을 두고:

```env
# ----- 운영 DB (실제 호스트/IP로 변경) -----
SPRING_DATASOURCE_URL=jdbc:postgresql://운영DB호스트:5432/monitoring
SPRING_DATASOURCE_USERNAME=monitoring
SPRING_DATASOURCE_PASSWORD=운영DB비밀번호

# 나머지 Aligo, Worker 등도 운영용 값으로 설정
POSTGRES_DB=monitoring
POSTGRES_USER=monitoring
POSTGRES_PASSWORD=운영DB비밀번호
# ...
```

그 다음:

```bash
docker-compose -f docker-compose.deploy.yml up -d
```

- `docker-compose.deploy.yml`은 **이미지만** 사용하고, DB는 **외부 운영 DB**를 쓸 수 있습니다.
- 이 경우 **postgres 서비스는 띄우지 않고**, Web/Worker만 운영 DB URL로 기동하면 됩니다.

### 3-2. postgres 서비스 없이 Web/Worker만 운영 DB로 실행

운영 DB를 쓰고, Docker Postgres는 쓰지 않는 예시입니다.

- **docker-compose.override.yml** 또는 별도 compose 파일에서 `postgres` 서비스를 제거하고, `SPRING_DATASOURCE_*` 만 운영 DB로 설정한 뒤:

```bash
# .env에 운영 DB만 설정
SPRING_DATASOURCE_URL=jdbc:postgresql://prod-db.company.com:5432/monitoring
SPRING_DATASOURCE_USERNAME=monitoring
SPRING_DATASOURCE_PASSWORD=실제비밀번호
```

- Web/Worker만 띄우는 compose 예시 (postgres 없음):

```yaml
# docker-compose.prod-only.yml 예시
version: '3.8'
services:
  web:
    image: monitoring-system-web:latest
    env_file: [ .env ]
    environment:
      SPRING_DATASOURCE_URL: ${SPRING_DATASOURCE_URL}
      SPRING_DATASOURCE_USERNAME: ${SPRING_DATASOURCE_USERNAME}
      SPRING_DATASOURCE_PASSWORD: ${SPRING_DATASOURCE_PASSWORD}
    ports: [ "${WEB_PORT:-8080}:8080" ]
    restart: unless-stopped
  worker:
    image: monitoring-system-worker:latest
    env_file: [ .env ]
    environment:
      SPRING_DATASOURCE_URL: ${SPRING_DATASOURCE_URL}
      SPRING_DATASOURCE_USERNAME: ${SPRING_DATASOURCE_USERNAME}
      SPRING_DATASOURCE_PASSWORD: ${SPRING_DATASOURCE_PASSWORD}
    restart: unless-stopped
```

- 실행:

```bash
docker-compose -f docker-compose.prod-only.yml up -d
```

---

## 4. .env 파일 분리 (테스트 / 운영 전환 편하게)

같은 머신에서 테스트와 운영을 번갈아 쓰려면 `.env`를 두 개 두고, 실행할 때만 지정할 수 있습니다.

- **`.env.local`** (테스트 – Docker Postgres)

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/monitoring
SPRING_DATASOURCE_USERNAME=monitoring
SPRING_DATASOURCE_PASSWORD=monitoring
APP_SYSTEM_NAME=모니터링(테스트)
```

- **`.env.prod`** (운영 – 실제 DB, Git에 넣지 말 것)

```env
SPRING_DATASOURCE_URL=jdbc:postgresql://운영DB호스트:5432/monitoring
SPRING_DATASOURCE_USERNAME=monitoring
SPRING_DATASOURCE_PASSWORD=실제비밀번호
APP_SYSTEM_NAME=CNIT 모니터링시스템
# ... 기타 운영용 설정
```

- **테스트** (Docker 전체):

```bash
docker-compose --env-file .env.local up -d
```

- **운영** (같은 서버에서 운영 DB로만 전환):

```bash
docker-compose -f docker-compose.deploy.yml --env-file .env.prod up -d
```

- `.env.local`, `.env.prod`는 `.gitignore`에 두어 커밋하지 않는 것을 권장합니다.

---

## 5. 정리

| 목적 | DB | 할 일 |
|------|-----|--------|
| **테스트** | localhost Docker PostgreSQL | `.env`에 DB 안 넣거나 `postgres:5432`(또는 localhost:5432)만 설정 후 `docker-compose up -d` |
| **운영** | 실제 운영 DB | `.env`(또는 `.env.prod`)에 `SPRING_DATASOURCE_URL=실제운영DB주소` 등 설정 후 동일 compose로 기동 |

애플리케이션 코드는 그대로 두고, **SPRING_DATASOURCE_URL / USERNAME / PASSWORD** 만 환경(테스트 vs 운영)에 맞게 바꾸면 됩니다.
