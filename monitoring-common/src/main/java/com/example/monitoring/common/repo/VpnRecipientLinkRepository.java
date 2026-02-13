package com.example.monitoring.common.repo;

import com.example.monitoring.common.domain.VpnRecipientLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VpnRecipientLinkRepository extends JpaRepository<VpnRecipientLinkEntity, Long> {

    @Query("SELECT l FROM VpnRecipientLinkEntity l JOIN FETCH l.recipient WHERE l.vpnId = :vpnId AND l.enabled = true")
    List<VpnRecipientLinkEntity> findByVpnIdAndEnabledTrue(@Param("vpnId") Long vpnId);

    @Query("SELECT l FROM VpnRecipientLinkEntity l JOIN FETCH l.recipient WHERE l.vpnId = :vpnId")
    List<VpnRecipientLinkEntity> findByVpnIdWithRecipient(@Param("vpnId") Long vpnId);

    List<VpnRecipientLinkEntity> findByVpnId(Long vpnId);
    
    java.util.Optional<VpnRecipientLinkEntity> findByVpnIdAndRecipient_Id(Long vpnId, Long recipientId);

    void deleteByVpnId(Long vpnId);
}
