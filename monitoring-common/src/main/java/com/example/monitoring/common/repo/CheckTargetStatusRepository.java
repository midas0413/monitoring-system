package com.example.monitoring.common.repo;

import com.example.monitoring.common.domain.CheckTargetStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CheckTargetStatusRepository extends JpaRepository<CheckTargetStatusEntity, Long> {
}
