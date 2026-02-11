package com.example.monitoring.web.service;

import com.example.monitoring.common.domain.VpnNotificationTemplateEntity;
import com.example.monitoring.common.repo.VpnNotificationTemplateRepository;
import com.example.monitoring.web.dto.VpnNotificationTemplateForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional
public class VpnNotificationTemplateService {

    private final VpnNotificationTemplateRepository templateRepo;

    public VpnNotificationTemplateService(VpnNotificationTemplateRepository templateRepo) {
        this.templateRepo = templateRepo;
    }

    @Transactional(readOnly = true)
    public List<VpnNotificationTemplateEntity> listByVpnId(Long vpnId) {
        return templateRepo.findByVpnIdOrderByNameAsc(vpnId);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<VpnNotificationTemplateEntity> getByVpnId(Long vpnId) {
        List<VpnNotificationTemplateEntity> templates = templateRepo.findByVpnIdOrderByNameAsc(vpnId);
        return templates.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(templates.get(0));
    }

    @Transactional(readOnly = true)
    public VpnNotificationTemplateEntity get(Long id) {
        return templateRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("VPN notification template not found: " + id));
    }

    @Transactional
    public Long create(VpnNotificationTemplateForm form) {
        // 필수 필드 검증
        if (!StringUtils.hasText(form.getName())) {
            throw new IllegalArgumentException("템플릿 이름은 필수입니다.");
        }
        if (!StringUtils.hasText(form.getTitleTemplate())) {
            throw new IllegalArgumentException("제목 템플릿은 필수입니다.");
        }
        if (!StringUtils.hasText(form.getBodyTemplate())) {
            throw new IllegalArgumentException("본문 템플릿은 필수입니다.");
        }
        
        // VPN당 하나의 템플릿만 허용
        List<VpnNotificationTemplateEntity> existing = templateRepo.findByVpnIdOrderByNameAsc(form.getVpnId());
        if (!existing.isEmpty()) {
            // 기존 템플릿이 있으면 업데이트
            VpnNotificationTemplateEntity entity = existing.get(0);
            applyForm(entity, form);
            templateRepo.save(entity);
            return entity.getId();
        }
        
        // 새 템플릿 생성
        VpnNotificationTemplateEntity entity = new VpnNotificationTemplateEntity();
        applyForm(entity, form);
        templateRepo.save(entity);
        return entity.getId();
    }

    @Transactional
    public void update(Long id, VpnNotificationTemplateForm form) {
        // 필수 필드 검증
        if (!StringUtils.hasText(form.getName())) {
            throw new IllegalArgumentException("템플릿 이름은 필수입니다.");
        }
        if (!StringUtils.hasText(form.getTitleTemplate())) {
            throw new IllegalArgumentException("제목 템플릿은 필수입니다.");
        }
        if (!StringUtils.hasText(form.getBodyTemplate())) {
            throw new IllegalArgumentException("본문 템플릿은 필수입니다.");
        }
        
        VpnNotificationTemplateEntity entity = get(id);
        
        // 이름 변경 시 중복 체크
        if (!entity.getName().equals(form.getName()) && 
            templateRepo.findByVpnIdAndName(form.getVpnId(), form.getName()).isPresent()) {
            throw new IllegalArgumentException("템플릿 이름 '" + form.getName() + "'이(가) 이미 존재합니다.");
        }
        
        applyForm(entity, form);
        templateRepo.save(entity);
    }

    @Transactional
    public void delete(Long id) {
        VpnNotificationTemplateEntity entity = get(id);
        templateRepo.delete(entity);
    }

    @Transactional(readOnly = true)
    public VpnNotificationTemplateForm toForm(VpnNotificationTemplateEntity entity) {
        VpnNotificationTemplateForm form = new VpnNotificationTemplateForm();
        form.setId(entity.getId());
        form.setVpnId(entity.getVpnId());
        form.setName(entity.getName());
        form.setTitleTemplate(entity.getTitleTemplate());
        form.setBodyTemplate(entity.getBodyTemplate());
        form.setEnabled(entity.getEnabled());
        form.setDescription(entity.getDescription());
        return form;
    }

    private void applyForm(VpnNotificationTemplateEntity entity, VpnNotificationTemplateForm form) {
        entity.setVpnId(form.getVpnId());
        // 필수 필드는 이미 검증되었으므로 trim만 수행
        entity.setName(form.getName() != null ? form.getName().trim() : null);
        entity.setTitleTemplate(form.getTitleTemplate() != null ? form.getTitleTemplate().trim() : null);
        entity.setBodyTemplate(form.getBodyTemplate() != null ? form.getBodyTemplate().trim() : null);
        entity.setEnabled(form.getEnabled() != null ? form.getEnabled() : true);
        entity.setDescription(StringUtils.hasText(form.getDescription()) ? form.getDescription().trim() : null);
    }
}
