package com.example.monitoring.common.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * 룰-수신자 링크. recipient는 alert_recipients 테이블을 참조.
 */
@Entity
@Table(
        name="alert_rule_recipient_links",
        uniqueConstraints = @UniqueConstraint(columnNames={"rule_id","recipient_id"})
)
public class AlertRuleRecipientLinkEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="rule_id", nullable=false)
    private Long ruleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="recipient_id", nullable=false)
    private AlertRecipientEntity recipient;  // alert_recipients 참조

    @Column(name="recipient_id", insertable=false, updatable=false)
    private Long recipientId;  // FK 값 (읽기 전용, N+1 방지)

    @Column(nullable=false)
    private Boolean enabled = true;

    @Column(name="created_at", nullable=false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public Long getId() { return id; }

    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }

    /** alert_recipients 엔티티 */
    public AlertRecipientEntity getRecipient() { return recipient; }
    public void setRecipient(AlertRecipientEntity recipient) { this.recipient = recipient; }

    /** recipient_id (alert_recipients.id) */
    public Long getRecipientId() { return recipientId != null ? recipientId : (recipient != null ? recipient.getId() : null); }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
}