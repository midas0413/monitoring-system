package com.example.monitoring.common.repo;

import com.example.monitoring.common.domain.CheckRunEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface CheckRunRepository extends JpaRepository<CheckRunEntity, Long> {

    List<CheckRunEntity> findTop100ByMonitoringRuleIdOrderByStartedAtDesc(Long monitoringRuleId);

    List<CheckRunEntity> findAllByOrderByStartedAtDesc(Pageable pageable);
    
    List<CheckRunEntity> findByMonitoringRuleIdAndStartedAtAfter(Long monitoringRuleId, OffsetDateTime startedAt);
}
