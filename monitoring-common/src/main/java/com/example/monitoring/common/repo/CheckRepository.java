package com.example.monitoring.common.repo;

import com.example.monitoring.common.domain.CheckEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CheckRepository extends JpaRepository<CheckEntity, Long> {

    List<CheckEntity> findByEnabledTrue();

    List<CheckEntity> findByTargetName(String targetName);

    List<CheckEntity> findByNameContainingIgnoreCaseOrderByIdDesc(String name);
}
