package com.example.monitoring.common.repo;

import com.example.monitoring.common.domain.TimezoneCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TimezoneCodeRepository extends JpaRepository<TimezoneCodeEntity, Long> {
    List<TimezoneCodeEntity> findByEnabledTrueOrderByDisplayOrderAsc();
    Optional<TimezoneCodeEntity> findByTimezoneId(String timezoneId);
}
