package com.example.monitoring.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Worker API 설정
 */
@Component
@ConfigurationProperties(prefix = "worker.api")
public class WorkerApiProperties {
    
    /**
     * Worker API Base URL
     * 예: http://worker-server:8081 또는 http://192.168.1.100:8081
     * 다른 서버에 배포 시 Worker 서버의 실제 IP/도메인으로 설정
     */
    private String baseUrl = "http://localhost:8081";
    
    /**
     * Worker API 사용 여부
     * true: Worker API를 통해 체크/테스트 실행
     * false: Web에서 직접 실행 (기존 방식)
     */
    private boolean enabled = true;
    
    /**
     * Worker API 인증 키 (선택)
     * Worker 서버에서 설정한 API 키와 일치해야 함
     * 설정하지 않으면 인증 없이 호출 (비권장)
     */
    private String apiKey;

    /**
     * Worker API 호출 타임아웃 (밀리초)
     * 기본값: 10000 (10초)
     */
    private int timeoutMs = 10000;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}
