package com.example.monitoring.common.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name="notification_outbox")
public class NotificationOutboxEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=20)
    private NotificationStatus status = NotificationStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false, length=20)
    private NotificationChannel channel;

    @Column(name="rule_id")
    private Long ruleId;

    @Column(name="check_run_id")
    private Long checkRunId;

    @Column(name="to_addr", nullable=false, length=200)
    private String toAddr;

    @Column(length=200)
    private String title;

    @Column(nullable=false, columnDefinition="text")
    private String body;

    @Column(nullable=false)
    private Integer attempt = 0;

    @Column(name="max_attempt", nullable=false)
    private Integer maxAttempt = 5;

    @Column(name="last_error", columnDefinition="text")
    private String lastError;

    @Column(name="next_attempt_at", nullable=false)
    private OffsetDateTime nextAttemptAt = OffsetDateTime.now();

    @Column(name="processing_by", length=100)
    private String processingBy;

    @Column(name="processing_until")
    private OffsetDateTime processingUntil;

    @Column(name="created_at", nullable=false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name="sent_at")
    private OffsetDateTime sentAt;

    public Long getId() { return id; }

    public NotificationStatus getStatus() { return status; }
    public void setStatus(NotificationStatus status) { this.status = status; }

    public NotificationChannel getChannel() { return channel; }
    public void setChannel(NotificationChannel channel) { this.channel = channel; }

    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }

    public Long getCheckRunId() { return checkRunId; }
    public void setCheckRunId(Long checkRunId) { this.checkRunId = checkRunId; }

    public String getToAddr() { return toAddr; }
    public void setToAddr(String toAddr) { this.toAddr = toAddr; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public Integer getAttempt() { return attempt; }
    public void setAttempt(Integer attempt) { this.attempt = attempt; }

    public Integer getMaxAttempt() { return maxAttempt; }
    public void setMaxAttempt(Integer maxAttempt) { this.maxAttempt = maxAttempt; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }

    public OffsetDateTime getNextAttemptAt() { return nextAttemptAt; }
    public void setNextAttemptAt(OffsetDateTime nextAttemptAt) { this.nextAttemptAt = nextAttemptAt; }

    public String getProcessingBy() { return processingBy; }
    public void setProcessingBy(String processingBy) { this.processingBy = processingBy; }

    public OffsetDateTime getProcessingUntil() { return processingUntil; }
    public void setProcessingUntil(OffsetDateTime processingUntil) { this.processingUntil = processingUntil; }

    public OffsetDateTime getCreatedAt() { return createdAt; }

    public OffsetDateTime getSentAt() { return sentAt; }
    public void setSentAt(OffsetDateTime sentAt) { this.sentAt = sentAt; }
}