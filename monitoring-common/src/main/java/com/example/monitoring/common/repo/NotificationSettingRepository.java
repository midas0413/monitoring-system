package com.example.monitoring.common.repo;

import com.example.monitoring.common.domain.NotificationSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationSettingRepository extends JpaRepository<NotificationSettingEntity, Long> {
    List<NotificationSettingEntity> findByEnabledTrue();
    Optional<NotificationSettingEntity> findByName(String name);
    List<NotificationSettingEntity> findByProviderAndEnabledTrue(String provider);
    boolean existsByName(String name);
}
