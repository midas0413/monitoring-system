package com.example.monitoring.web.service;

import com.example.monitoring.common.domain.SystemCodeEntity;
import com.example.monitoring.common.repo.SystemCodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class SystemCodeService {

    private final SystemCodeRepository repo;

    public SystemCodeService(SystemCodeRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<SystemCodeEntity> listByType(String codeType) {
        return repo.findByCodeTypeAndEnabledTrueOrderByDisplayOrderAsc(codeType);
    }

    @Transactional(readOnly = true)
    public List<SystemCodeEntity> listAllByType(String codeType) {
        return repo.findByCodeType(codeType);
    }

    @Transactional(readOnly = true)
    public Map<String, String> getCodeMap(String codeType) {
        return listByType(codeType).stream()
                .collect(Collectors.toMap(
                    SystemCodeEntity::getCodeValue,
                    SystemCodeEntity::getCodeLabel,
                    (v1, v2) -> v1
                ));
    }

    @Transactional(readOnly = true)
    public SystemCodeEntity getByTypeAndValue(String codeType, String codeValue) {
        return repo.findByCodeTypeAndEnabledTrueOrderByDisplayOrderAsc(codeType).stream()
                .filter(c -> c.getCodeValue().equals(codeValue))
                .findFirst()
                .orElse(null);
    }
}
