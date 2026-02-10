package com.example.monitoring.web.dto;

import com.example.monitoring.common.domain.ServerPurpose;
import java.util.ArrayList;
import java.util.List;

public class ServerForm {
    private Long id;
    private String name;
    private String host;
    private String timezone;
    private ServerPurpose serverPurpose;
    private Boolean enabled = true;
    private String description;
    
    // SSH 연결 정보 (SHELL, LOGS, DISK_SPACE 모니터링용)
    private Integer sshPort = 22;
    private String sshUsername;
    private String sshPassword;
    private String sshPrivateKeyPath;
    
    // VPN 연결 관리
    private List<Long> vpnIds = new ArrayList<>();  // 연결할 VPN ID 목록

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public ServerPurpose getServerPurpose() { return serverPurpose; }
    public void setServerPurpose(ServerPurpose serverPurpose) { this.serverPurpose = serverPurpose; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getSshPort() { return sshPort; }
    public void setSshPort(Integer sshPort) { this.sshPort = sshPort; }

    public String getSshUsername() { return sshUsername; }
    public void setSshUsername(String sshUsername) { this.sshUsername = sshUsername; }

    public String getSshPassword() { return sshPassword; }
    public void setSshPassword(String sshPassword) { this.sshPassword = sshPassword; }

    public String getSshPrivateKeyPath() { return sshPrivateKeyPath; }
    public void setSshPrivateKeyPath(String sshPrivateKeyPath) { this.sshPrivateKeyPath = sshPrivateKeyPath; }

    public List<Long> getVpnIds() { return vpnIds; }
    public void setVpnIds(List<Long> vpnIds) { this.vpnIds = vpnIds != null ? vpnIds : new ArrayList<>(); }
}
