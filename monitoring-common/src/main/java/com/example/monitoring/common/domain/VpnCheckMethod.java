package com.example.monitoring.common.domain;

/**
 * VPN 체크 방법
 */
public enum VpnCheckMethod {
    /**
     * TCP 연결만 체크 (포트 연결 테스트)
     */
    TCP,
    
    /**
     * PING(ICMP)만 체크
     */
    PING,
    
    /**
     * TCP와 PING 모두 체크 (둘 중 하나라도 성공하면 UP)
     */
    BOTH
}
