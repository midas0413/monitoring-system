package com.example.monitoring.web.service;

import com.example.monitoring.common.domain.KakaoTemplateEntity;
import com.example.monitoring.common.repo.KakaoTemplateRepository;
import com.example.monitoring.web.dto.KakaoTemplateForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class KakaoTemplateService {

    private static final Logger log = LoggerFactory.getLogger(KakaoTemplateService.class);

    private final KakaoTemplateRepository templateRepo;

    public KakaoTemplateService(KakaoTemplateRepository templateRepo) {
        this.templateRepo = templateRepo;
    }

    @Transactional(readOnly = true)
    public List<KakaoTemplateEntity> list() {
        return templateRepo.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<KakaoTemplateEntity> listEnabled() {
        return templateRepo.findByEnabledTrueOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public KakaoTemplateEntity get(Long id) {
        return templateRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("카카오 템플릿을 찾을 수 없습니다: " + id));
    }

    @Transactional(readOnly = true)
    public KakaoTemplateEntity getByTemplateCode(String templateCode) {
        return templateRepo.findByTemplateCode(templateCode)
                .orElse(null);
    }

    @Transactional
    public Long create(KakaoTemplateForm form) {
        // 템플릿 코드 중복 확인
        if (templateRepo.findByTemplateCode(form.getTemplateCode()).isPresent()) {
            throw new IllegalArgumentException("이미 등록된 템플릿 코드입니다: " + form.getTemplateCode());
        }

        // 템플릿 코드 길이 검증 (Aligo API 제한: 7바이트)
        String templateCode = form.getTemplateCode();
        if (templateCode != null) {
            int byteLength = templateCode.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            if (byteLength > 7) {
                throw new IllegalArgumentException("템플릿 코드는 7바이트 이하여야 합니다. 현재: " + byteLength + "바이트 (Aligo API 제한)");
            }
        }

        KakaoTemplateEntity entity = new KakaoTemplateEntity();
        applyForm(entity, form);
        templateRepo.save(entity);

        log.info("카카오 템플릿 생성: id={}, templateCode={}, name={}", 
                entity.getId(), entity.getTemplateCode(), entity.getName());
        return entity.getId();
    }

    @Transactional
    public void update(Long id, KakaoTemplateForm form) {
        KakaoTemplateEntity entity = get(id);

        // 템플릿 코드 변경 시 중복 확인
        if (!entity.getTemplateCode().equals(form.getTemplateCode())) {
            if (templateRepo.findByTemplateCode(form.getTemplateCode()).isPresent()) {
                throw new IllegalArgumentException("이미 등록된 템플릿 코드입니다: " + form.getTemplateCode());
            }
        }

        // 템플릿 코드 길이 검증 (Aligo API 제한: 7바이트)
        String templateCode = form.getTemplateCode();
        if (templateCode != null) {
            int byteLength = templateCode.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
            if (byteLength > 7) {
                throw new IllegalArgumentException("템플릿 코드는 7바이트 이하여야 합니다. 현재: " + byteLength + "바이트 (Aligo API 제한)");
            }
        }

        applyForm(entity, form);
        templateRepo.save(entity);

        log.info("카카오 템플릿 수정: id={}, templateCode={}, name={}", 
                id, entity.getTemplateCode(), entity.getName());
    }

    @Transactional
    public void delete(Long id) {
        if (!templateRepo.existsById(id)) {
            throw new IllegalArgumentException("카카오 템플릿을 찾을 수 없습니다: " + id);
        }
        templateRepo.deleteById(id);
        log.info("카카오 템플릿 삭제: id={}", id);
    }

    public KakaoTemplateForm toForm(KakaoTemplateEntity entity) {
        KakaoTemplateForm form = new KakaoTemplateForm();
        form.setId(entity.getId());
        form.setTemplateCode(entity.getTemplateCode());
        form.setName(entity.getName());
        form.setTemplateMessage(entity.getTemplateMessage());
        form.setVariables(entity.getVariables());
        form.setButtonInfo(entity.getButtonInfo());
        form.setEnabled(entity.getEnabled());
        return form;
    }

    private void applyForm(KakaoTemplateEntity entity, KakaoTemplateForm form) {
        entity.setTemplateCode(form.getTemplateCode());
        entity.setName(form.getName());
        entity.setTemplateMessage(form.getTemplateMessage());
        entity.setVariables(form.getVariables());
        entity.setButtonInfo(form.getButtonInfo());
        entity.setEnabled(form.getEnabled() != null ? form.getEnabled() : true);
    }
}
