package com.example.monitoring.web.service;

import com.example.monitoring.common.domain.CheckEntity;
import com.example.monitoring.web.dto.CheckForm;

import java.util.List;

public interface CheckService {
    List<CheckEntity> list(String q);
    CheckEntity get(Long id);
    Long create(CheckForm form);
    void update(Long id, CheckForm form);
    void delete(Long id);
    CheckForm toForm(CheckEntity e);
}