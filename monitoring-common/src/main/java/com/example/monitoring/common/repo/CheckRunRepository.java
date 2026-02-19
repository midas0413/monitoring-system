package com.example.monitoring.common.repo;

import com.example.monitoring.common.domain.CheckRunEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface CheckRunRepository extends JpaRepository<CheckRunEntity, Long> {

    List<CheckRunEntity> findTop100ByMonitoringRuleIdOrderByStartedAtDesc(Long monitoringRuleId);

    List<CheckRunEntity> findAllByOrderByStartedAtDesc(Pageable pageable);

    List<CheckRunEntity> findByMonitoringRuleIdAndStartedAtAfter(Long monitoringRuleId, OffsetDateTime startedAt);

    /** 직전 check run 조회 (동일 규칙, 현재 실행 시각 이전 중 가장 최근 1건) */
    Optional<CheckRunEntity> findTop1ByMonitoringRuleIdAndStartedAtBeforeOrderByStartedAtDesc(
            Long monitoringRuleId, OffsetDateTime startedAt);
}
