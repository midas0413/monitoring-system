package com.example.monitoring.web.dto;

public class VpnConnectionForm {
    private Long id;
    private String name;
    private String host;
    private Integer checkIntervalSec = 60;
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

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
