package com.example.monitoring.common.repo;

import com.example.monitoring.common.domain.NotificationOutboxEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class NotificationOutboxRepositoryCustomImpl implements NotificationOutboxRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    @Transactional
    public List<NotificationOutboxEntity> claimDue(OffsetDateTime now, int limit) {
        // Postgres 전용: UPDATE ... RETURNING
        String sql = """
            with cte as (
                select id
                  from notification_outbox
                 where status = 'PENDING'
                   and next_attempt_at <= :now
                 order by id
                 for update skip locked
                 limit :limit
            )
            update notification_outbox n
               set status = 'PROCESSING'
              from cte
             where n.id = cte.id
            returning n.*
        """;

        @SuppressWarnings("unchecked")
        List<NotificationOutboxEntity> claimed = em.createNativeQuery(sql, NotificationOutboxEntity.class)
                .setParameter("now", now)
                .setParameter("limit", limit)
                .getResultList();

        return claimed;
    }
}
