package com.example.monitoring.common.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "monitoring_rules")
public class MonitoringRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "server_id", nullable = false)
    private Long serverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "monitoring_type", nullable = false, length = 20)
    private MonitoringType monitoringType;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "interval_sec", nullable = false)
    private Integer intervalSec = 60;

    // SSH 공통 정보 (SHELL, LOGS, DISK_SPACE용)
    @Column(name = "ssh_port")
    private Integer sshPort;

    @Column(name = "ssh_username", length = 100)
    private String sshUsername;

    @Column(name = "ssh_password", columnDefinition = "text")
    private String sshPassword;

    @Column(name = "ssh_private_key_path", columnDefinition = "text")
    private String sshPrivateKeyPath;

    // DB 타입 전용 필드
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

    // SHELL 타입 전용
    @Column(name = "shell_script", columnDefinition = "text")
    private String shellScript;

    // LOGS 타입 전용
    @Column(name = "log_file_path", columnDefinition = "text")
    private String logFilePath;

    @Column(name = "include_keywords", columnDefinition = "text")
    private String includeKeywords;  // 쉼표 구분: ERROR, FAIL, CRITICAL

    @Column(name = "exclude_keywords", columnDefinition = "text")
    private String excludeKeywords;  // 쉼표 구분

    // DISK_SPACE 타입 전용
    @Column(name = "disk_path", length = 500)
    private String diskPath;

    // 알림 규칙
    @Enumerated(EnumType.STRING)
    @Column(name = "alert_operator", nullable = false, length = 40)
    private AlertOperator alertOperator;

    @Column(name = "threshold_num")
    private Double thresholdNum;

    @Column(name = "threshold_len")
    private Integer thresholdLen;

    @Column(columnDefinition = "text")
    private String pattern;  // 정규식 패턴

    // 알림 설정
    @Column(length = 100)
    private String channels;  // SMS,EMAIL,KAKAO

    @Column(name = "message_template", nullable = false, columnDefinition = "text")
    private String messageTemplate;

    @Column(name = "cooldown_sec", nullable = false)
    private Integer cooldownSec = 300;

    // 실행 관리
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

    @Column(name = "last_fired_at")
    private OffsetDateTime lastFiredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    // Getters and Setters
    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getServerId() { return serverId; }
    public void setServerId(Long serverId) { this.serverId = serverId; }

    public MonitoringType getMonitoringType() { return monitoringType; }
    public void setMonitoringType(MonitoringType monitoringType) { this.monitoringType = monitoringType; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public Integer getIntervalSec() { return intervalSec; }
    public void setIntervalSec(Integer intervalSec) { this.intervalSec = intervalSec; }

    public Integer getSshPort() { return sshPort; }
    public void setSshPort(Integer sshPort) { this.sshPort = sshPort; }

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

    public String getShellScript() { return shellScript; }
    public void setShellScript(String shellScript) { this.shellScript = shellScript; }

    public String getLogFilePath() { return logFilePath; }
    public void setLogFilePath(String logFilePath) { this.logFilePath = logFilePath; }

    public String getIncludeKeywords() { return includeKeywords; }
    public void setIncludeKeywords(String includeKeywords) { this.includeKeywords = includeKeywords; }

    public String getExcludeKeywords() { return excludeKeywords; }
    public void setExcludeKeywords(String excludeKeywords) { this.excludeKeywords = excludeKeywords; }

    public String getDiskPath() { return diskPath; }
    public void setDiskPath(String diskPath) { this.diskPath = diskPath; }

    public AlertOperator getAlertOperator() { return alertOperator; }
    public void setAlertOperator(AlertOperator alertOperator) { this.alertOperator = alertOperator; }

    public Double getThresholdNum() { return thresholdNum; }
    public void setThresholdNum(Double thresholdNum) { this.thresholdNum = thresholdNum; }

    public Integer getThresholdLen() { return thresholdLen; }
    public void setThresholdLen(Integer thresholdLen) { this.thresholdLen = thresholdLen; }

    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }

    public String getChannels() { return channels; }
    public void setChannels(String channels) { this.channels = channels; }

    public String getMessageTemplate() { return messageTemplate; }
    public void setMessageTemplate(String messageTemplate) { this.messageTemplate = messageTemplate; }

    public Integer getCooldownSec() { return cooldownSec; }
    public void setCooldownSec(Integer cooldownSec) { this.cooldownSec = cooldownSec; }

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

    public OffsetDateTime getLastFiredAt() { return lastFiredAt; }
    public void setLastFiredAt(OffsetDateTime lastFiredAt) { this.lastFiredAt = lastFiredAt; }

    public OffsetDateTime getCreatedAt() { return createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
