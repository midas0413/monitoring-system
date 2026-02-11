package com.example.monitoring.common.repo;

import com.example.monitoring.common.domain.VpnNotificationTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VpnNotificationTemplateRepository extends JpaRepository<VpnNotificationTemplateEntity, Long> {
    List<VpnNotificationTemplateEntity> findByVpnIdOrderByNameAsc(Long vpnId);
    List<VpnNotificationTemplateEntity> findByVpnIdAndEnabledTrueOrderByNameAsc(Long vpnId);
    Optional<VpnNotificationTemplateEntity> findByVpnIdAndName(Long vpnId, String name);
}
