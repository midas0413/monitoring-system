package com.example.monitoring.worker.alert;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Aligo API 설정 (카카오 알림톡, 문자)
 * 알리고 사이트: https://smartsms.aligo.in
 */
@ConfigurationProperties(prefix = "aligo")
public class AligoProperties {

    /** API Key (알리고 관리자 API 인증키) */
    private String apiKey;
    /** 사용자 ID (알리고 로그인 아이디) */
    private String userId;
    /** 발신번호 (사이트에 등록된 번호만 사용 가능) */
    private String sender;

    // 알림톡 전용 (선택)
    /** 발신프로필 키 (카카오 채널 연동 후 발급) */
    private String senderKey;
    /** 템플릿 코드 (알림톡 사용 시 필수, 알리고에서 승인된 템플릿) */
    private String templateCode;

    /** 테스트 모드 (Y: 실제 전송 없음, N: 실제 전송) */
    private String testMode = "N";

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getSenderKey() { return senderKey; }
    public void setSenderKey(String senderKey) { this.senderKey = senderKey; }

    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }

    public String getTestMode() { return testMode; }
    public void setTestMode(String testMode) { this.testMode = testMode; }

    public boolean isTestMode() {
        return "Y".equalsIgnoreCase(testMode);
    }

    public boolean isAlimtalkAvailable() {
        return StringUtils.hasText(apiKey) && StringUtils.hasText(userId) && StringUtils.hasText(sender)
                && StringUtils.hasText(senderKey) && StringUtils.hasText(templateCode);
    }

    public boolean isSmsAvailable() {
        return StringUtils.hasText(apiKey) && StringUtils.hasText(userId) && StringUtils.hasText(sender);
    }
}
