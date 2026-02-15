package com.example.monitoring.common.repo;

import com.example.monitoring.common.domain.ServerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServerRepository extends JpaRepository<ServerEntity, Long> {
    List<ServerEntity> findByEnabledTrueOrderByNameAsc();
    /** 연결상태 체크 주기가 설정된 서버 (interval > 0) */
    List<ServerEntity> findByEnabledTrueAndConnectionCheckIntervalSecGreaterThanOrderByNameAsc(int minInterval);
    Optional<ServerEntity> findByName(String name);
    boolean existsByName(String name);
}
