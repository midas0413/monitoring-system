package com.example.monitoring.common.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

// @Entity  // Deprecated: alert_rules 테이블이 monitoring_rules로 통합됨
// @Table(name = "alert_rules")
public class AlertRuleEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=100)
    private String name;

    @Column(nullable=false)
    private Boolean enabled = true;

    @Column(name="check_id")
    private Long checkId;

    @Enumerated(EnumType.STRING)
    @Column(name="rule_type", nullable=false, length=40)
    private AlertRuleType ruleType;

    @Column(name="threshold_num")
    private Double thresholdNum;

    @Column(name="threshold_len")
    private Integer thresholdLen;

    @Column(columnDefinition="text")
    private String pattern;

    @Column(length=100)
    private String channels; // optional

    @Column(name="message_template", nullable=false, columnDefinition="text")
    private String messageTemplate;

    @Column(name="cooldown_sec", nullable=false)
    private Integer cooldownSec = 300;

    @Column(name="last_fired_at")
    private OffsetDateTime lastFiredAt;

    @Column(name="created_at", nullable=false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public Long getCheckId() { return checkId; }
    public void setCheckId(Long checkId) { this.checkId = checkId; }

    public AlertRuleType getRuleType() { return ruleType; }
    public void setRuleType(AlertRuleType ruleType) { this.ruleType = ruleType; }

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

    public OffsetDateTime getLastFiredAt() { return lastFiredAt; }
    public void setLastFiredAt(OffsetDateTime lastFiredAt) { this.lastFiredAt = lastFiredAt; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
}