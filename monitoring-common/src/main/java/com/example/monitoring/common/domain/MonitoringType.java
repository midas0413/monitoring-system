package com.example.monitoring.common.domain;

public enum MonitoringType {
    SHELL,        // Shell 스크립트 실행
    DB,           // 데이터베이스 쿼리
    LOGS,         // 로그 파일 모니터링
    DISK_SPACE    // 디스크 공간 모니터링
}
