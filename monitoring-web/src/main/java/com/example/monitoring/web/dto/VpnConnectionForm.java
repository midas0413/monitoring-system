package com.example.monitoring.web.dto;

import com.example.monitoring.common.domain.VpnCheckMethod;

public class VpnConnectionForm {
    private Long id;
    private String name;
    private String host;
    private Integer checkIntervalSec = 60;
    private VpnCheckMethod checkMethod = VpnCheckMethod.TCP;
    private String kakaoTemplateCode;  // 카카오 알림톡 템플릿 ID
    private String kakaoTemplateVariables;  // 카카오 템플릿 변수 값 (JSON 형식: {"변수명": "값"})
    private Boolean enabled = true;
    private String description;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
