package com.example.monitoring.common.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "notification_settings")
public class NotificationSettingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @Column(nullable = false, length = 50)
    private String provider;  // ALIGO, SMTP, SLACK 등

    @Column(nullable = false)
    private Boolean enabled = true;

    // Aligo 설정
    @Column(name = "aligo_api_key", length = 200)
    private String aligoApiKey;

    @Column(name = "aligo_user_id", length = 100)
    private String aligoUserId;

    @Column(name = "aligo_sender", length = 50)
    private String aligoSender;

    @Column(name = "aligo_sender_key", length = 200)
    private String aligoSenderKey;

    @Column(name = "aligo_template_code", length = 100)
    private String aligoTemplateCode;

    @Column(name = "aligo_test_mode", length = 1)
    private String aligoTestMode = "N";  // Y: 테스트, N: 실제 전송

    // SMTP 설정
    @Column(name = "smtp_host", length = 255)
    private String smtpHost;

    @Column(name = "smtp_port")
    private Integer smtpPort;

    @Column(name = "smtp_username", length = 200)
    private String smtpUsername;

    @Column(name = "smtp_password", columnDefinition = "text")
    private String smtpPassword;

    @Column(name = "smtp_from_email", length = 200)
    private String smtpFromEmail;

    // 기타 설정 (JSON 형식)
    @Column(name = "extra_config", columnDefinition = "text")
    private String extraConfig;

    @Column(length = 500)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    // Getters and Setters
    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public String getAligoApiKey() { return aligoApiKey; }
    public void setAligoApiKey(String aligoApiKey) { this.aligoApiKey = aligoApiKey; }

    public String getAligoUserId() { return aligoUserId; }
    public void setAligoUserId(String aligoUserId) { this.aligoUserId = aligoUserId; }

    public String getAligoSender() { return aligoSender; }
    public void setAligoSender(String aligoSender) { this.aligoSender = aligoSender; }

    public String getAligoSenderKey() { return aligoSenderKey; }
    public void setAligoSenderKey(String aligoSenderKey) { this.aligoSenderKey = aligoSenderKey; }

    public String getAligoTemplateCode() { return aligoTemplateCode; }
    public void setAligoTemplateCode(String aligoTemplateCode) { this.aligoTemplateCode = aligoTemplateCode; }

    public String getAligoTestMode() { return aligoTestMode; }
    public void setAligoTestMode(String aligoTestMode) { this.aligoTestMode = aligoTestMode; }

    public String getSmtpHost() { return smtpHost; }
    public void setSmtpHost(String smtpHost) { this.smtpHost = smtpHost; }

    public Integer getSmtpPort() { return smtpPort; }
    public void setSmtpPort(Integer smtpPort) { this.smtpPort = smtpPort; }

    public String getSmtpUsername() { return smtpUsername; }
    public void setSmtpUsername(String smtpUsername) { this.smtpUsername = smtpUsername; }

    public String getSmtpPassword() { return smtpPassword; }
    public void setSmtpPassword(String smtpPassword) { this.smtpPassword = smtpPassword; }

    public String getSmtpFromEmail() { return smtpFromEmail; }
    public void setSmtpFromEmail(String smtpFromEmail) { this.smtpFromEmail = smtpFromEmail; }

    public String getExtraConfig() { return extraConfig; }
    public void setExtraConfig(String extraConfig) { this.extraConfig = extraConfig; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public OffsetDateTime getCreatedAt() { return createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
