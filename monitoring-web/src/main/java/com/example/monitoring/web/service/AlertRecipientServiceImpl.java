package com.example.monitoring.web.service;

import com.example.monitoring.common.domain.AlertRecipientEntity;
import com.example.monitoring.common.repo.AlertRecipientRepository;
import com.example.monitoring.web.dto.AlertRecipientForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional
public class AlertRecipientServiceImpl implements AlertRecipientService {

    private final AlertRecipientRepository repo;

    public AlertRecipientServiceImpl(AlertRecipientRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlertRecipientEntity> list(String q) {
        if (!StringUtils.hasText(q)) {
            // repo에 findAllByOrderByIdDesc()가 없을 수도 있으니 안전하게 처리
            return repo.findAll().stream()
                    .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                    .toList();
        }
        return repo.findByNameContainingIgnoreCaseOrderByIdDesc(q.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public AlertRecipientEntity get(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new IllegalStateException("Recipient not found: " + id));
    }

    @Override
    public Long create(AlertRecipientForm form) {
        AlertRecipientEntity e = new AlertRecipientEntity();
        applyForm(e, form);
        repo.save(e);
        return e.getId();
    }

    @Override
    public void update(Long id, AlertRecipientForm form) {
        AlertRecipientEntity e = get(id);
        applyForm(e, form);
        repo.save(e);
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AlertRecipientForm toForm(AlertRecipientEntity e) {
        AlertRecipientForm f = new AlertRecipientForm();
        f.setId(e.getId()); // 확장용(없어도 되지만 있으면 편함)
        f.setName(e.getName());
        f.setEnabled(e.getEnabled());
        f.setPhone(e.getPhone());
        f.setEmail(e.getEmail());
        f.setKakao(e.getKakao());
        f.loadFromChannelsCsv(e.getChannels());
        return f;
    }

    private void applyForm(AlertRecipientEntity e, AlertRecipientForm form) {
        e.setName(form.getName());
        e.setEnabled(form.getEnabled() != null ? form.getEnabled() : true);

        // channels CSV 저장 (form에서 normalize/중복제거 수행)
        e.setChannels(form.toChannelsCsv());

        e.setPhone(StringUtils.hasText(form.getPhone()) ? form.getPhone().trim() : null);
        e.setEmail(StringUtils.hasText(form.getEmail()) ? form.getEmail().trim() : null);
        e.setKakao(StringUtils.hasText(form.getKakao()) ? form.getKakao().trim() : null);
    }
}