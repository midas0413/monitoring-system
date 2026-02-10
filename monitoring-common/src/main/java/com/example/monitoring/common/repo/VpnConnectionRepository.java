package com.example.monitoring.common.repo;

import com.example.monitoring.common.domain.VpnConnectionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VpnConnectionRepository extends JpaRepository<VpnConnectionEntity, Long> {
    List<VpnConnectionEntity> findByEnabledTrueOrderByNameAsc();
    List<VpnConnectionEntity> findByEnabledTrue();
    Optional<VpnConnectionEntity> findByName(String name);
    List<VpnConnectionEntity> findByEnabledTrueAndStatus(String status);
}
