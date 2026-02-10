package com.example.monitoring.common.repo;

import com.example.monitoring.common.domain.SystemCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SystemCodeRepository extends JpaRepository<SystemCodeEntity, Long> {
    List<SystemCodeEntity> findByCodeTypeAndEnabledTrueOrderByDisplayOrderAsc(String codeType);
    List<SystemCodeEntity> findByCodeType(String codeType);
}
