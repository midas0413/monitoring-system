package com.example.monitoring.common.domain;

public enum CheckType {
    SQL,   // DB에서 실행
    SHELL  // worker가 서버에 ssh로 실행(다음 단계)
}