package com.example.monitoring.web.service;

import com.example.monitoring.common.domain.TimezoneCodeEntity;
import com.example.monitoring.common.repo.TimezoneCodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TimezoneService {

    private final TimezoneCodeRepository repository;

    public TimezoneService(TimezoneCodeRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<TimezoneCodeEntity> listEnabled() {
        try {
            return repository.findByEnabledTrueOrderByDisplayOrderAsc();
        } catch (Exception e) {
            // 타임존 테이블이 아직 생성되지 않은 경우 빈 리스트 반환
            return new java.util.ArrayList<>();
        }
    }

    @Transactional(readOnly = true)
    public TimezoneCodeEntity findByTimezoneId(String timezoneId) {
        return repository.findByTimezoneId(timezoneId).orElse(null);
    }
}
