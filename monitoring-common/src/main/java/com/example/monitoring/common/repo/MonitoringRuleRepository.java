package com.example.monitoring.common.repo;

import com.example.monitoring.common.domain.MonitoringRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface MonitoringRuleRepository extends JpaRepository<MonitoringRuleEntity, Long> {
    List<MonitoringRuleEntity> findByServerId(Long serverId);
    List<MonitoringRuleEntity> findByEnabledTrue();
    List<MonitoringRuleEntity> findByServerIdAndEnabledTrue(Long serverId);
    
    @Query("SELECT r FROM MonitoringRuleEntity r WHERE r.enabled = true AND r.nextRunAt <= :now AND (r.lockedUntil IS NULL OR r.lockedUntil <= :now) ORDER BY r.nextRunAt ASC")
    List<MonitoringRuleEntity> findDueRules(OffsetDateTime now);
    
    Optional<MonitoringRuleEntity> findByIdAndEnabledTrue(Long id);
}
