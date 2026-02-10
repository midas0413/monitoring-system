package com.example.monitoring.common.repo;

import com.example.monitoring.common.domain.ServerEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServerRepository extends JpaRepository<ServerEntity, Long> {
    List<ServerEntity> findByEnabledTrueOrderByNameAsc();
    Optional<ServerEntity> findByName(String name);
    boolean existsByName(String name);
}
