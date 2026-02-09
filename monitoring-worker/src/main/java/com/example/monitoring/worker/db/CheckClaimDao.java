package com.example.monitoring.worker.db;

import com.example.monitoring.common.domain.CheckEntity;
import com.example.monitoring.common.repo.CheckRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class CheckClaimDao {

    @PersistenceContext
    private EntityManager em;

    private final CheckRepository checkRepository;

    public CheckClaimDao(CheckRepository checkRepository) {
        this.checkRepository = checkRepository;
    }

    /** claim 후 CheckRepository로 재로딩하여 db_url 등 모든 컬럼이 올바르게 채워진 entity 반환 */
    @Transactional
    public List<CheckEntity> claimDueChecks(String workerId, int limit, int lockSeconds) {
        String sql = """
            with candidates as (
              select id
              from checks
              where enabled = true
                and next_run_at <= now()
                and (locked_until is null or locked_until < now())
              order by next_run_at asc
              limit :limit
              for update skip locked
            )
            update checks c
            set locked_until = now() + make_interval(secs => :lockSeconds),
                locked_by = :workerId
            from candidates
            where c.id = candidates.id
            returning c.id
            """;

        @SuppressWarnings("unchecked")
        List<Number> ids = em
                .createNativeQuery(sql)
                .setParameter("limit", limit)
                .setParameter("lockSeconds", lockSeconds)
                .setParameter("workerId", workerId)
                .getResultList();

        return ids.stream()
                .mapToLong(Number::longValue)
                .mapToObj(checkRepository::findById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .toList();
    }
}