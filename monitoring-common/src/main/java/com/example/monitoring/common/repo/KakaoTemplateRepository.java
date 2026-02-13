package com.example.monitoring.common.repo;

import com.example.monitoring.common.domain.KakaoTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KakaoTemplateRepository extends JpaRepository<KakaoTemplateEntity, Long> {
    List<KakaoTemplateEntity> findByEnabledTrueOrderByNameAsc();
    List<KakaoTemplateEntity> findAllByOrderByNameAsc();
    Optional<KakaoTemplateEntity> findByTemplateCode(String templateCode);
}
