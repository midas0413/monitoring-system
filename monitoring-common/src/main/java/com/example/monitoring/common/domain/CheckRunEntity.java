package com.example.monitoring.common.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "check_runs")
public class CheckRunEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "monitoring_rule_id", nullable = false)
    private Long monitoringRuleId;

    @Column(nullable = false)
    private Boolean success;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt = OffsetDateTime.now();

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "output", columnDefinition = "text")
    private String output;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    public Long getId() { return id; }

    public Long getMonitoringRuleId() { return monitoringRuleId; }
    public void setMonitoringRuleId(Long monitoringRuleId) { this.monitoringRuleId = monitoringRuleId; }
    
    // 하위 호환성을 위한 메서드 (deprecated)
    @Deprecated
    public Long getCheckId() { return monitoringRuleId; }
    @Deprecated
    public void setCheckId(Long checkId) { this.monitoringRuleId = checkId; }

    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }

    public OffsetDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(OffsetDateTime startedAt) { this.startedAt = startedAt; }

    public OffsetDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(OffsetDateTime finishedAt) { this.finishedAt = finishedAt; }

    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }

    public String getOutput() { return output; }
    public void setOutput(String output) { this.output = output; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
