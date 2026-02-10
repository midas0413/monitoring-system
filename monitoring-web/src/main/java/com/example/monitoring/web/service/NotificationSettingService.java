package com.example.monitoring.web.service;

import com.example.monitoring.common.domain.NotificationSettingEntity;
import com.example.monitoring.common.repo.NotificationSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional
public class NotificationSettingService {

    private static final Logger log = LoggerFactory.getLogger(NotificationSettingService.class);

    private final NotificationSettingRepository repo;

    public NotificationSettingService(NotificationSettingRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<NotificationSettingEntity> list(String q) {
        if (StringUtils.hasText(q)) {
            return repo.findAll().stream()
                    .filter(s -> s.getName().toLowerCase().contains(q.toLowerCase()) 
                             || (s.getProvider() != null && s.getProvider().toLowerCase().contains(q.toLowerCase())))
                    .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                    .toList();
        }
        return repo.findAll().stream()
                .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationSettingEntity get(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notification setting not found: " + id));
    }

    @Transactional(readOnly = true)
    public NotificationSettingEntity getByName(String name) {
        return repo.findByName(name)
                .orElseThrow(() -> new IllegalArgumentException("Notification setting not found: " + name));
    }

    @Transactional(readOnly = true)
    public List<NotificationSettingEntity> listEnabled() {
        return repo.findByEnabledTrue();
    }

    @Transactional(readOnly = true)
    public List<NotificationSettingEntity> listByProvider(String provider) {
        return repo.findByProviderAndEnabledTrue(provider);
    }

    @Transactional
    public Long create(NotificationSettingEntity entity) {
        if (repo.existsByName(entity.getName())) {
            throw new IllegalArgumentException("Notification setting name already exists: " + entity.getName());
        }
        repo.save(entity);
        log.info("Notification setting created: id={}, name={}, provider={}", 
                entity.getId(), entity.getName(), entity.getProvider());
        return entity.getId();
    }

    @Transactional
    public void update(Long id, NotificationSettingEntity entity) {
        NotificationSettingEntity existing = get(id);
        
        if (!existing.getName().equals(entity.getName()) && repo.existsByName(entity.getName())) {
            throw new IllegalArgumentException("Notification setting name already exists: " + entity.getName());
        }

        // 기존 엔티티 업데이트
        existing.setName(entity.getName());
        existing.setProvider(entity.getProvider());
        existing.setEnabled(entity.getEnabled());
        existing.setAligoApiKey(entity.getAligoApiKey());
        existing.setAligoUserId(entity.getAligoUserId());
        existing.setAligoSender(entity.getAligoSender());
        existing.setAligoSenderKey(entity.getAligoSenderKey());
        existing.setAligoTemplateCode(entity.getAligoTemplateCode());
        existing.setAligoTestMode(entity.getAligoTestMode());
        existing.setSmtpHost(entity.getSmtpHost());
        existing.setSmtpPort(entity.getSmtpPort());
        existing.setSmtpUsername(entity.getSmtpUsername());
        existing.setSmtpPassword(entity.getSmtpPassword());
        existing.setSmtpFromEmail(entity.getSmtpFromEmail());
        existing.setExtraConfig(entity.getExtraConfig());
        existing.setDescription(entity.getDescription());

        repo.save(existing);
        log.info("Notification setting updated: id={}, name={}", id, existing.getName());
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new IllegalArgumentException("Notification setting not found: " + id);
        }
        repo.deleteById(id);
        log.info("Notification setting deleted: id={}", id);
    }
}
