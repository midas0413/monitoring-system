package com.example.monitoring.common.repo;

import com.example.monitoring.common.domain.CheckRunEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface CheckRunRepository extends JpaRepository<CheckRunEntity, Long> {

    // Deprecated: checkId는 monitoringRuleId로 변경됨. 하위 호환성을 위해 @Query 사용
    @Deprecated
    @Query(value = "SELECT * FROM check_runs WHERE monitoring_rule_id = :checkId ORDER BY started_at DESC LIMIT 100", nativeQuery = true)
    List<CheckRunEntity> findTop100ByCheckIdOrderByStartedAtDesc(@Param("checkId") Long checkId);
    
    List<CheckRunEntity> findTop100ByMonitoringRuleIdOrderByStartedAtDesc(Long monitoringRuleId);

    List<CheckRunEntity> findAllByOrderByStartedAtDesc(Pageable pageable);
    
    List<CheckRunEntity> findByMonitoringRuleIdAndStartedAtAfter(Long monitoringRuleId, OffsetDateTime startedAt);
}
