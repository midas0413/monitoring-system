package com.example.monitoring.web.dto;

public class VpnNotificationTemplateForm {
    private Long id;
    private Long vpnId;
    private String name;
    private String titleTemplate;
    private String bodyTemplate;
    private Boolean enabled = true;
    private String description;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getVpnId() { return vpnId; }
    public void setVpnId(Long vpnId) { this.vpnId = vpnId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTitleTemplate() { return titleTemplate; }
    public void setTitleTemplate(String titleTemplate) { this.titleTemplate = titleTemplate; }

    public String getBodyTemplate() { return bodyTemplate; }
    public void setBodyTemplate(String bodyTemplate) { this.bodyTemplate = bodyTemplate; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
