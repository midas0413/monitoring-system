package com.example.monitoring.common.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * 카카오 알림톡 템플릿 엔티티
 * Aligo 템플릿 코드 및 변수 정보 관리
 */
@Entity
@Table(name = "kakao_templates")
public class KakaoTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_code", nullable = false, unique = true, length = 100)
    private String templateCode;  // Aligo 템플릿 코드 (tpl_code)

    @Column(nullable = false, length = 100)
    private String name;  // 템플릿 이름

    @Column(name = "template_message", columnDefinition = "text")
    private String templateMessage;  // 템플릿 메시지 형태 (알리고에 등록된 템플릿 본문 형태)

    @Column(columnDefinition = "text")
    private String variables;  // 템플릿 변수 목록 (JSON 형식 또는 쉼표 구분)

    @Column(name = "button_info", columnDefinition = "text")
    private String buttonInfo;  // 버튼 정보 (JSON 형식)

    @Column(nullable = false)
    private Boolean enabled = true;  // 활성화 여부

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    // Getters and Setters
    public Long getId() { return id; }

    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTemplateMessage() { return templateMessage; }
    public void setTemplateMessage(String templateMessage) { this.templateMessage = templateMessage; }

    public String getVariables() { return variables; }
    public void setVariables(String variables) { this.variables = variables; }

    public String getButtonInfo() { return buttonInfo; }
    public void setButtonInfo(String buttonInfo) { this.buttonInfo = buttonInfo; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public OffsetDateTime getCreatedAt() { return createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
