package com.example.monitoring.common.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "checks")
public class CheckEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private CheckType type;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "target_name", nullable = false, length = 100)
    private String targetName;

    @Column(nullable = false)
    private String host;

    @Column(name = "timezone", length = 50)
    private String timezone;

    // SHELL용
    @Column
    private Integer port;

    @Column(name = "ssh_username", length = 100)
    private String sshUsername;

    @Column(name = "ssh_password", columnDefinition = "text")
    private String sshPassword;

    @Column(name = "ssh_private_key_path", columnDefinition = "text")
    private String sshPrivateKeyPath;

    // SQL용
    @Column(name = "db_type", length = 20)
    private String dbType;

    @Column(name = "db_port")
    private Integer dbPort;

    @Column(name = "db_name", length = 100)
    private String dbName;

    @Column(name = "db_url", columnDefinition = "text")
    private String dbUrl;

    @Column(name = "db_username", length = 100)
    private String dbUsername;

    @Column(name = "db_password", columnDefinition = "text")
    private String dbPassword;

    @Column(nullable = false, columnDefinition = "text")
    private String script;

    @Column(name = "interval_sec", nullable = false)
    private Integer intervalSec = 60;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "next_run_at", nullable = false)
    private OffsetDateTime nextRunAt = OffsetDateTime.now();

    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;

    @Column(name = "locked_by", length = 100)
    private String lockedBy;

    @Column(name = "last_run_at")
    private OffsetDateTime lastRunAt;

    @Column(name = "last_status", length = 20)
    private String lastStatus;

    @Column(name = "last_checked_at")
    private OffsetDateTime lastCheckedAt;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ServerStatus status = ServerStatus.UNKNOWN;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // getters/setters
    public Long getId() { return id; }

    public CheckType getType() { return type; }
    public void setType(CheckType type) { this.type = type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

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

    public String getDbUrl() { return dbUrl; }
    public void setDbUrl(String dbUrl) { this.dbUrl = dbUrl; }

    public String getDbUsername() { return dbUsername; }
    public void setDbUsername(String dbUsername) { this.dbUsername = dbUsername; }

    public String getDbPassword() { return dbPassword; }
    public void setDbPassword(String dbPassword) { this.dbPassword = dbPassword; }

    public String getScript() { return script; }
    public void setScript(String script) { this.script = script; }

    public Integer getIntervalSec() { return intervalSec; }
    public void setIntervalSec(Integer intervalSec) { this.intervalSec = intervalSec; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public OffsetDateTime getNextRunAt() { return nextRunAt; }
    public void setNextRunAt(OffsetDateTime nextRunAt) { this.nextRunAt = nextRunAt; }

    public OffsetDateTime getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(OffsetDateTime lockedUntil) { this.lockedUntil = lockedUntil; }

    public String getLockedBy() { return lockedBy; }
    public void setLockedBy(String lockedBy) { this.lockedBy = lockedBy; }

    public OffsetDateTime getLastRunAt() { return lastRunAt; }
    public void setLastRunAt(OffsetDateTime lastRunAt) { this.lastRunAt = lastRunAt; }

    public String getLastStatus() { return lastStatus; }
    public void setLastStatus(String lastStatus) { this.lastStatus = lastStatus; }

    public OffsetDateTime getLastCheckedAt() { return lastCheckedAt; }
    public void setLastCheckedAt(OffsetDateTime lastCheckedAt) { this.lastCheckedAt = lastCheckedAt; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }

    public ServerStatus getStatus() { return status; }
    public void setStatus(ServerStatus status) { this.status = status; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
}
