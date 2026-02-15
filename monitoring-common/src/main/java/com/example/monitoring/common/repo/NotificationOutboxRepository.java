package com.example.monitoring.common.repo;

import com.example.monitoring.common.domain.NotificationOutboxEntity;
import com.example.monitoring.common.domain.NotificationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface NotificationOutboxRepository
        extends JpaRepository<NotificationOutboxEntity, Long>, NotificationOutboxRepositoryCustom {

    @Query("""
        select n from NotificationOutboxEntity n
         where n.status = :status
           and n.nextAttemptAt <= :now
         order by n.id
    """)
    List<NotificationOutboxEntity> findDue(
            @Param("status") NotificationStatus status,
            @Param("now") OffsetDateTime now,
            Pageable pageable
    );

    List<NotificationOutboxEntity> findByOrderByIdDesc(Pageable pageable);

    /** 체크 실행 ID로 알림 내역 조회 (체크 실행 내역과 연동 확인용) */
    List<NotificationOutboxEntity> findByCheckRunIdOrderByIdDesc(Long checkRunId, Pageable pageable);

    // Deprecated: checkId는 monitoringRuleId로 변경됨. 하위 호환성을 위해 @Query 수정
    @Query("SELECT COUNT(n) FROM NotificationOutboxEntity n WHERE n.checkRunId IN (SELECT cr.id FROM CheckRunEntity cr WHERE cr.monitoringRuleId = :checkId) AND n.status = :status")
    long countSentByCheckId(@Param("checkId") Long checkId, @Param("status") NotificationStatus status);

    @Query("SELECT COUNT(n) FROM NotificationOutboxEntity n WHERE n.monitoringRuleId = :ruleId AND n.status = :status")
    long countSentByMonitoringRuleId(@Param("ruleId") Long ruleId, @Param("status") NotificationStatus status);
}