package com.example.monitoring.common.repo;

import com.example.monitoring.common.domain.NotificationOutboxEntity;

import java.time.OffsetDateTime;
import java.util.List;

public interface NotificationOutboxRepositoryCustom {

    /**
     * 멀티워커 안전 선점:
     * - PENDING 이면서 next_attempt_at <= now 인 것 중 limit개를
     * - FOR UPDATE SKIP LOCKED 로 잡고
     * - status=PROCESSING 으로 업데이트하면서 RETURNING으로 엔티티 반환
     */
    List<NotificationOutboxEntity> claimDue(OffsetDateTime now, int limit);
}