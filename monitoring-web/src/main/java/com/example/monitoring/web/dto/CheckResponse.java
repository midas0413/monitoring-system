package com.example.monitoring.web.dto;

import com.example.monitoring.common.domain.CheckEntity;
import com.example.monitoring.common.domain.CheckType;

import java.time.OffsetDateTime;

public record CheckResponse(
        Long id,
        String targetName,
        CheckType type,
        String name,
        String script,
        Integer intervalSec,
        Boolean enabled,
        OffsetDateTime createdAt
) {
    public static CheckResponse from(CheckEntity e) {
        return new CheckResponse(
                e.getId(),
                e.getTargetName(),
                e.getType(),
                e.getName(),
                e.getScript(),
                e.getIntervalSec(),
                e.getEnabled(),
                e.getCreatedAt()
        );
    }
}
