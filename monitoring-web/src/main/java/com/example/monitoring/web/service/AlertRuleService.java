package com.example.monitoring.web.service;

import com.example.monitoring.common.domain.AlertRuleEntity;
import com.example.monitoring.common.domain.AlertRuleType;
import com.example.monitoring.common.domain.CheckEntity;
// import com.example.monitoring.common.repo.AlertRuleRepository;  // Deprecated
// import com.example.monitoring.common.repo.CheckRepository;  // Deprecated
import com.example.monitoring.web.dto.AlertRuleForm;
import com.example.monitoring.web.dto.CheckForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// @Service  // Deprecated: AlertRuleService는 alert_rules 테이블이 monitoring_rules로 통합되어 더 이상 사용되지 않음
public class AlertRuleService {

    // Deprecated: alert_rules와 checks 테이블이 monitoring_rules로 통합됨
    // private final AlertRuleRepository ruleRepo;
    // private final CheckRepository checkRepo;
    private final CheckService checkService;

    public AlertRuleService(/* AlertRuleRepository ruleRepo, CheckRepository checkRepo, */ CheckService checkService) {
        // this.ruleRepo = ruleRepo;
        // this.checkRepo = checkRepo;
        this.checkService = checkService;
    }

    @Transactional(readOnly = true)
    public List<AlertRuleEntity> list(String q) {
        // Deprecated: AlertRuleRepository 사용 불가
        // if (StringUtils.hasText(q)) return ruleRepo.findByNameContainingIgnoreCaseOrderByIdDesc(q.trim());
        // return ruleRepo.findAllByOrderByIdDesc();
        return new java.util.ArrayList<>();
    }

    @Transactional(readOnly = true)
    public AlertRuleEntity get(Long id) {
        // Deprecated: AlertRuleRepository 사용 불가
        // return ruleRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Rule not found: " + id));
        throw new IllegalArgumentException("Rule not found: " + id);
    }

    /** Check 1개 + Rule 1개 동시 생성 */
    @Transactional
    public Long create(AlertRuleForm f) {
        // Deprecated: AlertRuleRepository 사용 불가
        throw new UnsupportedOperationException("AlertRuleService.create is deprecated");
        /*
        CheckForm cf = toCheckForm(f);
        Long checkId = checkService.create(cf);

        AlertRuleEntity e = new AlertRuleEntity();
        applyRule(e, f);
        e.setCheckId(checkId);
        ruleRepo.save(e);
        return e.getId();
        */
    }

    /** Check + Rule 동시 수정. checkId 없으면 Check 신규 생성 후 연결 */
    @Transactional
    public void update(Long id, AlertRuleForm f) {
        // Deprecated: AlertRuleRepository 사용 불가
        throw new UnsupportedOperationException("AlertRuleService.update is deprecated");
        /*
        AlertRuleEntity rule = get(id);
        Long checkId = rule.getCheckId();

        if (checkId == null) {
            CheckForm cf = toCheckForm(f);
            checkId = checkService.create(cf);
            rule.setCheckId(checkId);
        } else {
            CheckForm cf = toCheckForm(f);
            checkService.update(checkId, cf);
        }

        applyRule(rule, f);
        ruleRepo.save(rule);
        */
    }

    /** Rule 삭제 후 연결된 Check 삭제 */
    @Transactional
    public void delete(Long id) {
        // Deprecated: AlertRuleRepository 사용 불가
        throw new UnsupportedOperationException("AlertRuleService.delete is deprecated");
        /*
        AlertRuleEntity rule = get(id);
        Long checkId = rule.getCheckId();
        ruleRepo.deleteById(id);
        if (checkId != null) {
            checkRepo.deleteById(checkId);
        }
        */
    }

    @Transactional(readOnly = true)
    public AlertRuleForm toForm(AlertRuleEntity rule) {
        AlertRuleForm f = new AlertRuleForm();
        f.setId(rule.getId());
        f.setName(rule.getName());
        f.setEnabled(rule.getEnabled());
        f.setCheckId(rule.getCheckId());
        f.setRuleType(rule.getRuleType() != null ? rule.getRuleType().name() : null);
        f.setThresholdNum(rule.getThresholdNum());
        f.setThresholdLen(rule.getThresholdLen());
        f.setPattern(rule.getPattern());
        f.setMessageTemplate(rule.getMessageTemplate());
        f.setCooldownSec(rule.getCooldownSec());
        f.loadFromChannelsCsv(rule.getChannels());

        // Deprecated: CheckRepository 사용 불가
        // if (rule.getCheckId() != null) {
        //     CheckEntity c = checkRepo.findById(rule.getCheckId()).orElse(null);
        //     if (c != null) {
        //         f.setType(c.getType() != null ? c.getType().name() : "SHELL");
        //         f.setIntervalSec(c.getIntervalSec());
        //         f.setTargetName(c.getTargetName());
        //         f.setHost(c.getHost());
        //         f.setTimezone(c.getTimezone());
        //         f.setPort(c.getPort());
        //         f.setSshUsername(c.getSshUsername());
        //         f.setSshPassword(c.getSshPassword());
        //         f.setSshPrivateKeyPath(c.getSshPrivateKeyPath());
        //         f.setDbType(c.getDbType());
        //         f.setDbPort(c.getDbPort());
        //         f.setDbName(c.getDbName());
        //         f.setDbUsername(c.getDbUsername());
        //         f.setDbPassword(c.getDbPassword());
        //         f.setScript(c.getScript());
        //         f.setSqlText(c.getScript());
        //     }
        // }
        return f;
    }

    private CheckForm toCheckForm(AlertRuleForm f) {
        CheckForm cf = new CheckForm();
        cf.setName(f.getName());
        cf.setEnabled(f.getEnabled());
        cf.setType(f.getType());
        cf.setIntervalSec(f.getIntervalSec());
        cf.setTargetName(f.getTargetName());
        cf.setHost(f.getHost());
        cf.setTimezone(f.getTimezone());
        cf.setPort(f.getPort());
        cf.setSshUsername(f.getSshUsername());
        cf.setSshPassword(f.getSshPassword());
        cf.setSshPrivateKeyPath(f.getSshPrivateKeyPath());
        cf.setDbType(f.getDbType());
        cf.setDbPort(f.getDbPort());
        cf.setDbName(f.getDbName());
        cf.setDbUsername(f.getDbUsername());
        cf.setDbPassword(f.getDbPassword());
        cf.setScript(f.getScript());
        cf.setSqlText(f.getSqlText());
        return cf;
    }

    private void applyRule(AlertRuleEntity e, AlertRuleForm f) {
        e.setName(f.getName());
        e.setEnabled(f.getEnabled() != null ? f.getEnabled() : Boolean.TRUE);
        e.setRuleType(StringUtils.hasText(f.getRuleType()) ? AlertRuleType.valueOf(f.getRuleType().trim().toUpperCase()) : AlertRuleType.RUN_FAILED);
        e.setThresholdNum(f.getThresholdNum());
        e.setThresholdLen(f.getThresholdLen());
        e.setPattern(f.getPattern());
        e.setMessageTemplate(f.getMessageTemplate());
        e.setCooldownSec(f.getCooldownSec() != null ? f.getCooldownSec() : 300);
        String channelsCsv = f.toChannelsCsv();
        e.setChannels(StringUtils.hasText(channelsCsv) ? channelsCsv : null);
    }

    @Transactional(readOnly = true)
    public Map<Long, String> buildCheckDisplayMap(List<AlertRuleEntity> rules) {
        Map<Long, String> map = new HashMap<>();
        // Deprecated: CheckRepository 사용 불가
        for (AlertRuleEntity r : rules) {
            if (r.getCheckId() == null) continue;
            if (map.containsKey(r.getCheckId())) continue;
            // String display = checkRepo.findById(r.getCheckId())
            //         .map(c -> "[" + c.getId() + "] " + (c.getName() != null ? c.getName() : ""))
            //         .orElse("[" + r.getCheckId() + "] -");
            map.put(r.getCheckId(), "[" + r.getCheckId() + "] -");
        }
        return map;
    }

    @Transactional(readOnly = true)
    public Map<Long, String> buildServerDisplayMap(List<AlertRuleEntity> rules) {
        Map<Long, String> map = new HashMap<>();
        // Deprecated: CheckRepository 사용 불가
        for (AlertRuleEntity r : rules) {
            if (r.getCheckId() == null) continue;
            if (map.containsKey(r.getCheckId())) continue;
            // String display = checkRepo.findById(r.getCheckId())
            //         .map(c -> c.getTargetName() != null ? c.getTargetName() : "-")
            //         .orElse("-");
            map.put(r.getCheckId(), "-");
        }
        return map;
    }

    @Transactional(readOnly = true)
    public Map<Long, String> buildTimezoneDisplayMap(List<AlertRuleEntity> rules) {
        Map<Long, String> map = new HashMap<>();
        // Deprecated: CheckRepository 사용 불가
        for (AlertRuleEntity r : rules) {
            if (r.getCheckId() == null) continue;
            if (map.containsKey(r.getCheckId())) continue;
            // String display = checkRepo.findById(r.getCheckId())
            //         .map(c -> c.getTimezone() != null ? c.getTimezone() : "-")
            //         .orElse("-");
            map.put(r.getCheckId(), "-");
        }
        return map;
    }
}