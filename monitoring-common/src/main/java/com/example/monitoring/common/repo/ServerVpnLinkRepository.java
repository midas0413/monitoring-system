package com.example.monitoring.common.repo;

import com.example.monitoring.common.domain.ServerVpnLinkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ServerVpnLinkRepository extends JpaRepository<ServerVpnLinkEntity, Long> {
    List<ServerVpnLinkEntity> findByServerId(Long serverId);
    List<ServerVpnLinkEntity> findByVpnId(Long vpnId);
    List<ServerVpnLinkEntity> findByServerIdAndEnabledTrue(Long serverId);
    List<ServerVpnLinkEntity> findByVpnIdAndEnabledTrue(Long vpnId);
    void deleteByServerId(Long serverId);
    void deleteByVpnId(Long vpnId);
}
