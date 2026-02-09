package com.example.monitoring.web.dto;

/**
 * Check 등록 폼. type에 따라 SHELL은 SSH 정보, SQL은 DB 연결 정보만 사용.
 */
public class CheckForm {
    private String name;
    private String type;          // SHELL / SQL
    private Integer intervalSec;
    private Boolean enabled = true;

    private String script;        // SHELL/SQL 공용 (script 또는 sqlText 중 저장)
    private String sqlText;

    // 연결 대상 정보 (type별로 일부만 사용)
    private String targetName;    // 표시명 (ex: DEV_DBMS)

    // 공통
    private String host;
    private String timezone;

    // SHELL용
    private Integer port;
    private String sshUsername;
    private String sshPassword;
    private String sshPrivateKeyPath;

    // SQL용
    private String dbType;
    private Integer dbPort;
    private String dbName;
    private String dbUsername;
    private String dbPassword;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getIntervalSec() { return intervalSec; }
    public void setIntervalSec(Integer intervalSec) { this.intervalSec = intervalSec; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public String getScript() { return script; }
    public void setScript(String script) { this.script = script; }

    public String getSqlText() { return sqlText; }
    public void setSqlText(String sqlText) { this.sqlText = sqlText; }

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
}
