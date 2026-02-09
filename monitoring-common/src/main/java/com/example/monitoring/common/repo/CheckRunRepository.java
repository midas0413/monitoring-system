package com.example.monitoring.common.repo;

import com.example.monitoring.common.domain.CheckRunEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CheckRunRepository extends JpaRepository<CheckRunEntity, Long> {

    List<CheckRunEntity> findTop100ByCheckIdOrderByStartedAtDesc(Long checkId);

    List<CheckRunEntity> findAllByOrderByStartedAtDesc(Pageable pageable);
}
