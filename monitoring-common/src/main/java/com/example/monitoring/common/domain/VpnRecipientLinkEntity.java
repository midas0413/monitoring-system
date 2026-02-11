package com.example.monitoring.common.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * VPN-수신자 링크. VPN 상태 변경 알림을 받을 수신자 지정.
 */
@Entity
@Table(
    name = "vpn_recipient_links",
    uniqueConstraints = @UniqueConstraint(columnNames = {"vpn_id", "recipient_id"})
)
public class VpnRecipientLinkEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vpn_id", nullable = false)
    private Long vpnId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private AlertRecipientEntity recipient;

    @Column(name = "recipient_id", insertable = false, updatable = false)
    private Long recipientId;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public Long getId() { return id; }

    public Long getVpnId() { return vpnId; }
    public void setVpnId(Long vpnId) { this.vpnId = vpnId; }

    public AlertRecipientEntity getRecipient() { return recipient; }
    public void setRecipient(AlertRecipientEntity recipient) { this.recipient = recipient; }

    public Long getRecipientId() {
        return recipientId != null ? recipientId : (recipient != null ? recipient.getId() : null);
    }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
}
