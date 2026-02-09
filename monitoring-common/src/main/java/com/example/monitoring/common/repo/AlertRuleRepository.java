package com.example.monitoring.common.repo;

import com.example.monitoring.common.domain.AlertRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AlertRuleRepository extends JpaRepository<AlertRuleEntity, Long> {
    List<AlertRuleEntity> findByEnabledTrue();
    List<AlertRuleEntity> findByNameContainingIgnoreCaseOrderByIdDesc(String name);
    List<AlertRuleEntity> findAllByOrderByIdDesc();
    Optional<AlertRuleEntity> findFirstByCheckId(Long checkId);
}