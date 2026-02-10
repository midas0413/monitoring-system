package com.example.monitoring.worker.db;

import com.example.monitoring.common.domain.MonitoringRuleEntity;
import com.example.monitoring.common.repo.MonitoringRuleRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 모니터링 룰 선점(claim) DAO
 * 멀티워커 환경에서 안전하게 룰을 선점하여 실행
 */
@Repository
public class MonitoringRuleClaimDao {

    @PersistenceContext
    private EntityManager em;

    private final MonitoringRuleRepository ruleRepository;

    public MonitoringRuleClaimDao(MonitoringRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    /**
     * 실행 대상 모니터링 룰 선점
     * @param workerId 워커 ID
     * @param limit 최대 선점 개수
     * @param lockSeconds 락 유지 시간(초)
     * @return 선점된 모니터링 룰 목록
     */
    @Transactional
    public List<MonitoringRuleEntity> claimDueRules(String workerId, int limit, int lockSeconds) {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime lockedUntil = now.plusSeconds(lockSeconds);
        
        // Native Query로 선점 (SKIP LOCKED 사용)
        // VPN이 다운된 서버의 룰은 제외
        // LEFT JOIN과 FOR UPDATE를 함께 사용할 수 없으므로 NOT EXISTS를 사용
        String sql = """
            WITH candidates AS (
              SELECT mr.id
              FROM monitoring_rules mr
              INNER JOIN servers s ON mr.server_id = s.id
              WHERE mr.enabled = true
                AND mr.next_run_at <= :now
                AND (mr.locked_until IS NULL OR mr.locked_until <= :now)
                AND NOT EXISTS (
                  -- VPN을 사용하고 있고, 그 VPN이 DOWN인 경우 제외
                  SELECT 1
                  FROM server_vpn_links svl
                  INNER JOIN vpn_connections vpn ON svl.vpn_id = vpn.id
                  WHERE svl.server_id = s.id
                    AND svl.enabled = true
                    AND vpn.enabled = true
                    AND vpn.status = 'DOWN'
                )
              ORDER BY mr.next_run_at ASC
              LIMIT :limit
              FOR UPDATE SKIP LOCKED
            )
            UPDATE monitoring_rules mr
            SET locked_until = :lockedUntil,
                locked_by = :workerId
            FROM candidates
            WHERE mr.id = candidates.id
            RETURNING mr.id
            """;

        @SuppressWarnings("unchecked")
        List<Number> ids = em
                .createNativeQuery(sql)
                .setParameter("now", now)
                .setParameter("lockedUntil", lockedUntil)
                .setParameter("limit", limit)
                .setParameter("workerId", workerId)
                .getResultList();

        // 선점된 ID로 전체 엔티티 조회
        return ids.stream()
                .mapToLong(Number::longValue)
                .mapToObj(ruleRepository::findById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .toList();
    }
}
