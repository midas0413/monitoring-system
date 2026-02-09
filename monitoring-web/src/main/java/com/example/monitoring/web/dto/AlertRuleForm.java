package com.example.monitoring.web.dto;

/**
 * Alert Rule + Check 통합 폼. Check 1개당 Rule 1개.
 */
public class AlertRuleForm {
    private Long id;
    private String name;
    private Boolean enabled = true;

    private Long checkId;  // 편집 시에만 사용, 신규 시 null

    private String ruleType;
    private Double thresholdNum;
    private Integer thresholdLen;
    private String pattern;
    private String messageTemplate;
    private Integer cooldownSec = 300;

    // === Check 필드 ===
    private String type = "SHELL";        // SHELL / SQL
    private Integer intervalSec = 60;
    private String targetName;
    private String host;
    private String timezone;
    private Integer port;
    private String sshUsername;
    private String sshPassword;
    private String sshPrivateKeyPath;
    private String dbType;
    private Integer dbPort;
    private String dbName;
    private String dbUsername;
    private String dbPassword;
    private String script;
    private String sqlText;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public Long getCheckId() { return checkId; }
    public void setCheckId(Long checkId) { this.checkId = checkId; }

    public String getRuleType() { return ruleType; }
    public void setRuleType(String ruleType) { this.ruleType = ruleType; }

    public Double getThresholdNum() { return thresholdNum; }
    public void setThresholdNum(Double thresholdNum) { this.thresholdNum = thresholdNum; }

    public Integer getThresholdLen() { return thresholdLen; }
    public void setThresholdLen(Integer thresholdLen) { this.thresholdLen = thresholdLen; }

    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }

    public String getMessageTemplate() { return messageTemplate; }
    public void setMessageTemplate(String messageTemplate) { this.messageTemplate = messageTemplate; }

    public Integer getCooldownSec() { return cooldownSec; }
    public void setCooldownSec(Integer cooldownSec) { this.cooldownSec = cooldownSec; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getIntervalSec() { return intervalSec; }
    public void setIntervalSec(Integer intervalSec) { this.intervalSec = intervalSec; }

    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

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

    public String getScript() { return script; }
    public void setScript(String script) { this.script = script; }

    public String getSqlText() { return sqlText; }
    public void setSqlText(String sqlText) { this.sqlText = sqlText; }
}