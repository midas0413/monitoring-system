package com.example.monitoring.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class KakaoTemplateForm {
    private Long id;

    @NotBlank(message = "템플릿 코드를 입력해주세요.")
    @Size(max = 100, message = "템플릿 코드는 100자 이내로 입력해주세요.")
    private String templateCode;

    @NotBlank(message = "템플릿 이름을 입력해주세요.")
    @Size(max = 100, message = "템플릿 이름은 100자 이내로 입력해주세요.")
    private String name;

    private String templateMessage;  // 템플릿 메시지 형태

    private String variables;  // 템플릿 변수 목록 (JSON 형식 또는 쉼표 구분)

    private String buttonInfo;  // 버튼 정보 (JSON 형식)

    private Boolean enabled = true;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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
}
