# 멀티스테이지 빌드를 사용하여 web과 worker 이미지 생성
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

# Gradle 캐시를 활용하기 위해 의존성 파일 먼저 복사
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY monitoring-common/build.gradle.kts ./monitoring-common/
COPY monitoring-web/build.gradle.kts ./monitoring-web/
COPY monitoring-worker/build.gradle.kts ./monitoring-worker/

# 의존성 다운로드 (캐시 활용)
RUN gradle dependencies --no-daemon || true

# 소스 코드 복사
COPY monitoring-common ./monitoring-common
COPY monitoring-web ./monitoring-web
COPY monitoring-worker ./monitoring-worker

# 빌드 실행
RUN gradle clean build -x test --no-daemon

# Web 애플리케이션 이미지
FROM eclipse-temurin:17-jre-alpine AS web

# Health check를 위한 wget 설치
RUN apk add --no-cache wget

WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=builder /app/monitoring-web/build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/ || exit 1

# 애플리케이션 실행
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]

# Worker 애플리케이션 이미지
FROM eclipse-temurin:17-jre-alpine AS worker

WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=builder /app/monitoring-worker/build/libs/*.jar app.jar

# 애플리케이션 실행
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
