package com.example.monitoring.common.domain;

public enum AlertOperator {
    RUN_FAILED,              // 실행 실패
    OUTPUT_NUM_GT,          // 출력값 > 임계값
    OUTPUT_NUM_LT,          // 출력값 < 임계값
    OUTPUT_LEN_GT,          // 출력 길이 > 임계값
    OUTPUT_LEN_LT,          // 출력 길이 < 임계값
    OUTPUT_CONTAINS,        // 출력값 포함
    OUTPUT_NOT_CONTAINS,    // 출력값 미포함
    OUTPUT_MATCHES          // 출력값 정규식 매칭
}
