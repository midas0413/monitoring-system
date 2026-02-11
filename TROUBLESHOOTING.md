# 문제 해결 가이드

## PostgreSQL 연결 오류

### 증상
```
Connection to localhost:5432 refused. Check that the hostname and port are correct and that the postmaster is accepting TCP/IP connections.
```

### 원인
- PostgreSQL 서버가 실행되지 않음
- 데이터베이스 연결 정보(호스트, 포트, 사용자명, 비밀번호)가 잘못됨
- 방화벽이나 네트워크 설정 문제

### 해결 방법

#### 1. 환경 변수로 데이터베이스 연결 정보 설정

**Windows (PowerShell)**
```powershell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://172.22.1.113:5432/monitoring"
$env:SPRING_DATASOURCE_USERNAME="monitoring"
$env:SPRING_DATASOURCE_PASSWORD="monitoring"

.\gradlew :monitoring-worker:bootRun
```

**Windows (CMD)**
```cmd
set SPRING_DATASOURCE_URL=jdbc:postgresql://172.22.1.113:5432/monitoring
set SPRING_DATASOURCE_USERNAME=monitoring
set SPRING_DATASOURCE_PASSWORD=monitoring

gradlew :monitoring-worker:bootRun
```

**Linux/Mac**
```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://172.22.1.113:5432/monitoring
export SPRING_DATASOURCE_USERNAME=monitoring
export SPRING_DATASOURCE_PASSWORD=monitoring

./gradlew :monitoring-worker:bootRun
```

#### 2. application.yml 파일 직접 수정

`monitoring-worker/src/main/resources/application.yml` 파일에서:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://172.22.1.113:5432/monitoring  # 실제 DB 서버 주소로 변경
    username: monitoring
    password: monitoring
```

#### 3. PostgreSQL 서버 상태 확인

**PostgreSQL이 실행 중인지 확인:**
```bash
# Windows
netstat -an | findstr 5432

# Linux/Mac
netstat -an | grep 5432
# 또는
ss -tuln | grep 5432
```

**PostgreSQL 서버 시작 (로컬인 경우):**
```bash
# Windows (서비스로 실행 중인 경우)
net start postgresql-x64-15

# Linux
sudo systemctl start postgresql
# 또는
sudo service postgresql start
```

#### 4. Docker Compose로 PostgreSQL 실행

프로젝트 루트에서:
```bash
# PostgreSQL만 실행
docker-compose up -d postgres

# 모든 서비스 실행
docker-compose up -d
```

#### 5. 연결 테스트

**psql 클라이언트로 직접 연결 테스트:**
```bash
psql -h 172.22.1.113 -p 5432 -U monitoring -d monitoring
```

**telnet으로 포트 확인:**
```bash
# Windows
telnet 172.22.1.113 5432

# Linux/Mac
nc -zv 172.22.1.113 5432
```

## 기타 문제

### Flyway 마이그레이션 오류

Flyway가 데이터베이스 스키마를 초기화할 수 없는 경우:

1. 데이터베이스가 존재하는지 확인
2. 사용자 권한 확인
3. `application.yml`에서 Flyway 설정 확인:
   ```yaml
   spring:
     flyway:
       enabled: true
       baseline-on-migrate: true
       validate-on-migrate: false
   ```

### Worker ID 중복 오류

여러 Worker가 동일한 ID를 사용하는 경우:

환경 변수로 고유한 Worker ID 설정:
```bash
export WORKER_ID=worker-1  # 각 Worker마다 고유한 ID
```

### 메모리 부족 오류

JVM 힙 메모리 증가:
```bash
export GRADLE_OPTS="-Xmx2g"
./gradlew :monitoring-worker:bootRun
```
