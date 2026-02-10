package com.example.monitoring.web.service;

import com.example.monitoring.common.domain.MonitoringRuleEntity;
import com.example.monitoring.common.domain.ServerEntity;
import com.example.monitoring.common.repo.MonitoringRuleRepository;
import com.example.monitoring.common.repo.ServerRepository;
import com.example.monitoring.web.dto.MonitoringRuleForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class MonitoringRuleService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringRuleService.class);

    private final MonitoringRuleRepository ruleRepo;
    private final ServerRepository serverRepo;

    public MonitoringRuleService(MonitoringRuleRepository ruleRepo, ServerRepository serverRepo) {
        this.ruleRepo = ruleRepo;
        this.serverRepo = serverRepo;
    }

    @Transactional(readOnly = true)
    public List<MonitoringRuleEntity> list(String q) {
        if (StringUtils.hasText(q)) {
            return ruleRepo.findAll().stream()
                    .filter(r -> r.getName().toLowerCase().contains(q.toLowerCase()))
                    .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                    .toList();
        }
        return ruleRepo.findAll().stream()
                .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public MonitoringRuleEntity get(Long id) {
        return ruleRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Monitoring rule not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<MonitoringRuleEntity> listByServerId(Long serverId) {
        return ruleRepo.findByServerId(serverId);
    }

    @Transactional
    public Long create(MonitoringRuleForm form) {
        // 서버 존재 확인
        if (!serverRepo.existsById(form.getServerId())) {
            throw new IllegalArgumentException("Server not found: " + form.getServerId());
        }

        MonitoringRuleEntity entity = new MonitoringRuleEntity();
        applyForm(entity, form);
        ruleRepo.save(entity);

        log.info("Monitoring rule created: id={}, name={}, serverId={}", 
                entity.getId(), entity.getName(), entity.getServerId());
        return entity.getId();
    }

    @Transactional
    public void update(Long id, MonitoringRuleForm form) {
        MonitoringRuleEntity entity = get(id);
        
        // 서버 존재 확인
        if (!serverRepo.existsById(form.getServerId())) {
            throw new IllegalArgumentException("Server not found: " + form.getServerId());
        }

        applyForm(entity, form);
        ruleRepo.save(entity);

        log.info("Monitoring rule updated: id={}, name={}", id, entity.getName());
    }

    @Transactional
    public void delete(Long id) {
        if (!ruleRepo.existsById(id)) {
            throw new IllegalArgumentException("Monitoring rule not found: " + id);
        }
        ruleRepo.deleteById(id);
        log.info("Monitoring rule deleted: id={}", id);
    }

    @Transactional(readOnly = true)
    public Map<Long, String> buildServerDisplayMap(List<MonitoringRuleEntity> rules) {
        Map<Long, String> map = new HashMap<>();
        for (MonitoringRuleEntity rule : rules) {
            if (rule.getServerId() != null) {
                ServerEntity server = serverRepo.findById(rule.getServerId()).orElse(null);
                map.put(rule.getId(), server != null ? server.getName() : "?");
            } else {
                map.put(rule.getId(), "-");
            }
        }
        return map;
    }

    private void applyForm(MonitoringRuleEntity entity, MonitoringRuleForm form) {
        entity.setName(form.getName());
        entity.setServerId(form.getServerId());
        entity.setMonitoringType(form.getMonitoringType());
        entity.setEnabled(form.getEnabled() != null ? form.getEnabled() : true);
        entity.setIntervalSec(form.getIntervalSec() != null ? form.getIntervalSec() : 60);

        // SSH 정보는 서버에서 가져옴 (SHELL, LOGS, DISK_SPACE 타입에서 사용)
        // MonitoringRuleEntity에는 저장하지 않고, 실행 시 서버 정보에서 가져옴
        // 따라서 여기서는 SSH 정보를 저장하지 않음

        // DB 정보
        entity.setDbType(form.getDbType());
        entity.setDbPort(form.getDbPort());
        entity.setDbName(form.getDbName());
        // DB URL 자동 생성 (서버의 host 사용)
        ServerEntity server = serverRepo.findById(form.getServerId()).orElse(null);
        String host = server != null ? server.getHost() : null;
        entity.setDbUrl(buildDbUrl(host, form.getDbType(), form.getDbPort(), form.getDbName()));
        entity.setDbUsername(form.getDbUsername());
        // 비밀번호는 빈 값이 아닐 때만 업데이트
        if (form.getDbPassword() != null && !form.getDbPassword().isBlank()) {
            entity.setDbPassword(form.getDbPassword());
        }

        // 타입별 필드
        // DB 타입일 때는 sqlScript를 shellScript 필드에 저장 (공용 필드로 사용)
        if (form.getMonitoringType() != null && form.getMonitoringType().name().equals("DB")) {
            entity.setShellScript(form.getSqlScript());  // DB 타입일 때 SQL Script 저장
        } else {
            entity.setShellScript(form.getShellScript());  // SHELL 타입일 때 Shell Script 저장
        }
        entity.setLogFilePath(form.getLogFilePath());
        entity.setIncludeKeywords(form.getIncludeKeywords());
        entity.setExcludeKeywords(form.getExcludeKeywords());
        entity.setDiskPath(form.getDiskPath());

        // 알림 규칙
        entity.setAlertOperator(form.getAlertOperator());
        entity.setThresholdNum(form.getThresholdNum());
        entity.setThresholdLen(form.getThresholdLen());
        entity.setPattern(form.getPattern());

        // 알림 설정
        entity.setChannels(form.toChannelsCsv());
        entity.setMessageTemplate(form.getMessageTemplate() != null ? form.getMessageTemplate() : 
                "${ruleName}에 모니터링 알림이 발생하였습니다.\n임계값 : ${threshold}\n현재값 : ${outputNum}");
        entity.setCooldownSec(form.getCooldownSec() != null ? form.getCooldownSec() : 300);
    }

    public MonitoringRuleForm toForm(MonitoringRuleEntity entity) {
        MonitoringRuleForm form = new MonitoringRuleForm();
        form.setId(entity.getId());
        form.setName(entity.getName());
        form.setServerId(entity.getServerId());
        form.setMonitoringType(entity.getMonitoringType());
        form.setEnabled(entity.getEnabled());
        form.setIntervalSec(entity.getIntervalSec());

        // SSH 정보는 서버에서 가져오므로 form에 설정하지 않음

        form.setDbType(entity.getDbType());
        form.setDbPort(entity.getDbPort());
        form.setDbName(entity.getDbName());
        // DB URL은 자동 생성되므로 form에 설정하지 않음
        form.setDbUsername(entity.getDbUsername());
        form.setDbPassword(entity.getDbPassword());

        // shellScript 필드는 타입에 따라 shellScript 또는 sqlScript로 매핑
        if (entity.getMonitoringType() != null && entity.getMonitoringType().name().equals("DB")) {
            form.setSqlScript(entity.getShellScript());  // DB 타입일 때 SQL Script로 로드
        } else {
            form.setShellScript(entity.getShellScript());  // SHELL 타입일 때 Shell Script로 로드
        }
        form.setLogFilePath(entity.getLogFilePath());
        form.setIncludeKeywords(entity.getIncludeKeywords());
        form.setExcludeKeywords(entity.getExcludeKeywords());
        form.setDiskPath(entity.getDiskPath());

        form.setAlertOperator(entity.getAlertOperator());
        form.setThresholdNum(entity.getThresholdNum());
        form.setThresholdLen(entity.getThresholdLen());
        form.setPattern(entity.getPattern());

        form.loadFromChannelsCsv(entity.getChannels());
        form.setMessageTemplate(entity.getMessageTemplate());
        form.setCooldownSec(entity.getCooldownSec());

        return form;
    }

    /**
     * DB URL 자동 생성
     */
    private String buildDbUrl(String host, String dbType, Integer dbPort, String dbName) {
        if (!StringUtils.hasText(host) || !StringUtils.hasText(dbType) || dbPort == null || !StringUtils.hasText(dbName)) {
            return null;
        }
        String subprotocol = switch (dbType.toLowerCase()) {
            case "postgresql" -> "postgresql";
            case "mysql" -> "mysql";
            case "oracle" -> "oracle:thin";
            case "mssql" -> "sqlserver";
            default -> dbType.toLowerCase();
        };
        if ("oracle:thin".equals(subprotocol)) {
            return "jdbc:oracle:thin:@" + host + ":" + dbPort + ":" + dbName;
        }
        return "jdbc:" + subprotocol + "://" + host + ":" + dbPort + "/" + dbName;
    }
}
