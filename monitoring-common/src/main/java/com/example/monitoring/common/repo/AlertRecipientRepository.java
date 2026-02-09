package com.example.monitoring.common.repo;

import com.example.monitoring.common.domain.AlertRecipientEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRecipientRepository extends JpaRepository<AlertRecipientEntity, Long> {

    List<AlertRecipientEntity> findByEnabledTrue();

    // 간단 검색(이름 like) 용도. 더 고급 검색은 QueryDSL/Specification으로 확장.
    List<AlertRecipientEntity> findByNameContainingIgnoreCaseOrderByIdDesc(String name);

    List<AlertRecipientEntity> findAllByOrderByIdDesc();
    List<AlertRecipientEntity> findByEnabledTrueOrderByIdDesc();
}