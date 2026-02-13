package com.example.monitoring.common.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "vpn_connections")
public class VpnConnectionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @Column(nullable = false)
    private String host;

    @Column(name = "check_interval_sec", nullable = false)
    private Integer checkIntervalSec = 60;

    @Enumerated(EnumType.STRING)
    @Column(name = "check_method", nullable = false, length = 10)
    private VpnCheckMethod checkMethod = VpnCheckMethod.TCP;

    @Column(name = "kakao_template_code", length = 100)
    private String kakaoTemplateCode;  // 카카오 알림톡 템플릿 ID (Aligo tpl_code)

    @Column(name = "kakao_template_variables", columnDefinition = "text")
    private String kakaoTemplateVariables;  // 카카오 템플릿 변수 값 (JSON 형식: {"변수명": "값"})

    @Column(nullable = false)
    private Boolean enabled = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ServerStatus status = ServerStatus.UNKNOWN;

    @Column(name = "last_checked_at")
    private OffsetDateTime lastCheckedAt;

    @Column(name = "last_status_change_at")
    private OffsetDateTime lastStatusChangeAt;

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

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public Integer getCheckIntervalSec() { return checkIntervalSec; }
    public void setCheckIntervalSec(Integer checkIntervalSec) { this.checkIntervalSec = checkIntervalSec; }

    public VpnCheckMethod getCheckMethod() { return checkMethod; }
    public void setCheckMethod(VpnCheckMethod checkMethod) { this.checkMethod = checkMethod; }

    public String getKakaoTemplateCode() { return kakaoTemplateCode; }
    public void setKakaoTemplateCode(String kakaoTemplateCode) { this.kakaoTemplateCode = kakaoTemplateCode; }

    public String getKakaoTemplateVariables() { return kakaoTemplateVariables; }
    public void setKakaoTemplateVariables(String kakaoTemplateVariables) { this.kakaoTemplateVariables = kakaoTemplateVariables; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public ServerStatus getStatus() { return status; }
    public void setStatus(ServerStatus status) { this.status = status; }

    public OffsetDateTime getLastCheckedAt() { return lastCheckedAt; }
    public void setLastCheckedAt(OffsetDateTime lastCheckedAt) { this.lastCheckedAt = lastCheckedAt; }

    public OffsetDateTime getLastStatusChangeAt() { return lastStatusChangeAt; }
    public void setLastStatusChangeAt(OffsetDateTime lastStatusChangeAt) { this.lastStatusChangeAt = lastStatusChangeAt; }

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
