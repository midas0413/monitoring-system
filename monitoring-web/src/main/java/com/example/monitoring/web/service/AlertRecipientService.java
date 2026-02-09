package com.example.monitoring.web.service;

import com.example.monitoring.common.domain.AlertRecipientEntity;
import com.example.monitoring.web.dto.AlertRecipientForm;

import java.util.List;

public interface AlertRecipientService {

    List<AlertRecipientEntity> list(String q);

    AlertRecipientEntity get(Long id);

    Long create(AlertRecipientForm form);

    void update(Long id, AlertRecipientForm form);

    void delete(Long id);

    // ✅ Entity -> Form 변환
    AlertRecipientForm toForm(AlertRecipientEntity entity);
}