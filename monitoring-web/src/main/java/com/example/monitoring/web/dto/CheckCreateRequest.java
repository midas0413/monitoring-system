package com.example.monitoring.web.dto;

import com.example.monitoring.common.domain.CheckType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CheckCreateRequest {

    @NotNull
    private CheckType type;

    @NotBlank
    private String name;

    @NotBlank
    private String targetName;

    @NotBlank
    private String host;

    @NotBlank
    private String script;

    @Min(1)
    private Integer intervalSec = 60;

    private Boolean enabled = true;

    // SHELL용
    private Integer port = 22;
    private String sshUsername;
    private String sshPassword;
    private String sshPrivateKeyPath;

    // SQL용
    private String dbType;
    private Integer dbPort;
    private String dbName;
    private String dbUsername;
    private String dbPassword;

    public CheckType getType() { return type; }
    public void setType(CheckType type) { this.type = type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public String getScript() { return script; }
    public void setScript(String script) { this.script = script; }

    public Integer getIntervalSec() { return intervalSec; }
    public void setIntervalSec(Integer intervalSec) { this.intervalSec = intervalSec; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public Integer getPort() { return port; }
    public void setPort(Integer port) { this.port = port; }
    public String getSshUsername() { return sshUsername; }
    public void setSshUsername(String sshUsername) { this.sshUsername = sshUsername; }
    public String getSshPassword() { return sshPassword; }
    public void setSshPassword(String sshPassword) { this.sshPassword = sshPassword; }
    public String getSshPrivateKeyPath() { return sshPrivateKeyPath; }
    public void setSshPrivateKeyPath(String sshPrivateKeyPath) { this.sshPrivateKeyPath = sshPrivateKeyPath; }

    public String getDbType() { return dbType; }
    public void setDbType(String dbType) { this.dbType = dbType; }
    public Integer getDbPort() { return dbPort; }
    public void setDbPort(Integer dbPort) { this.dbPort = dbPort; }
    public String getDbName() { return dbName; }
    public void setDbName(String dbName) { this.dbName = dbName; }
    public String getDbUsername() { return dbUsername; }
    public void setDbUsername(String dbUsername) { this.dbUsername = dbUsername; }
    public String getDbPassword() { return dbPassword; }
    public void setDbPassword(String dbPassword) { this.dbPassword = dbPassword; }
}
