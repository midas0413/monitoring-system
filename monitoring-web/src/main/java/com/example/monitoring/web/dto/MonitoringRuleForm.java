package com.example.monitoring.web.dto;

import com.example.monitoring.common.domain.AlertOperator;
import com.example.monitoring.common.domain.MonitoringType;
import java.util.ArrayList;
import java.util.List;

public class MonitoringRuleForm {
    private Long id;
    private String name;
    private Long serverId;
    private MonitoringType monitoringType;
    private Boolean enabled = true;
    private Integer intervalSec = 60;

    // SSH 정보는 서버에서 가져오므로 제거됨

    // DB 타입 전용 필드
    private String dbType;
    private Integer dbPort;
    private String dbName;
    // DB URL은 자동 생성되므로 필드 제거
    private String dbUsername;
    private String dbPassword;

    // SHELL 타입 전용
    private String shellScript;

    // DB 타입 전용
    private String sqlScript;

    // LOGS 타입 전용
    private String logFilePath;
    private String includeKeywords;  // 쉼표 구분: ERROR, FAIL, CRITICAL
    private String excludeKeywords;  // 쉼표 구분

    // DISK_SPACE 타입 전용
    private String diskPath;

    // 알림 규칙
    private AlertOperator alertOperator;
    private Double thresholdNum;
    private Integer thresholdLen;
    private String pattern;  // 정규식 패턴

    // 알림 설정
    private List<String> channelList = new ArrayList<>();  // SMS,EMAIL,KAKAO
    private String messageTemplate;
    private Integer cooldownSec = 300;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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

    public String getShellScript() { return shellScript; }
    public void setShellScript(String shellScript) { this.shellScript = shellScript; }

    public String getSqlScript() { return sqlScript; }
    public void setSqlScript(String sqlScript) { this.sqlScript = sqlScript; }

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

    public List<String> getChannelList() { return channelList; }
    public void setChannelList(List<String> channelList) {
        this.channelList = (channelList == null) ? new ArrayList<>() : channelList;
    }

    public String getMessageTemplate() { return messageTemplate; }
    public void setMessageTemplate(String messageTemplate) { this.messageTemplate = messageTemplate; }

    public Integer getCooldownSec() { return cooldownSec; }
    public void setCooldownSec(Integer cooldownSec) { this.cooldownSec = cooldownSec; }

    /** DB의 channels(CSV: "SMS,EMAIL") → form.channelList 로딩 */
    public void loadFromChannelsCsv(String csv) {
        this.channelList.clear();
        if (csv == null || csv.isBlank()) return;
        for (String s : csv.split(",")) {
            String v = s.trim().toUpperCase();
            if (!v.isBlank() && !this.channelList.contains(v)) {
                this.channelList.add(v);
            }
        }
    }

    /** form.channelList → DB 저장용 CSV("SMS,EMAIL") */
    public String toChannelsCsv() {
        if (channelList == null || channelList.isEmpty()) return "";
        List<String> normalized = new ArrayList<>();
        for (String s : channelList) {
            if (s == null) continue;
            String v = s.trim().toUpperCase();
            if (!v.isBlank() && !normalized.contains(v)) normalized.add(v);
        }
        return String.join(",", normalized);
    }
}
