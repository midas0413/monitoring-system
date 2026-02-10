package com.example.monitoring.web.service;

import com.example.monitoring.common.domain.CheckEntity;
import com.example.monitoring.common.domain.CheckType;
// import com.example.monitoring.common.repo.CheckRepository;  // Deprecated
import com.example.monitoring.web.dto.CheckForm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

// @Service  // Deprecated: CheckServiceImpl은 checks 테이블이 monitoring_rules로 통합되어 더 이상 사용되지 않음
@Transactional
public class CheckServiceImpl implements CheckService {

    // Deprecated: CheckRepository는 더 이상 사용되지 않음
    // private final CheckRepository repo;

    public CheckServiceImpl(/* CheckRepository repo */) {
        // this.repo = repo;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CheckEntity> list(String q) {
        // Deprecated: CheckRepository 사용 불가
        throw new UnsupportedOperationException("CheckServiceImpl.list is deprecated");
        /*
        if (!StringUtils.hasText(q)) {
            return repo.findAll().stream()
                    .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                    .toList();
        }
        return repo.findByNameContainingIgnoreCaseOrderByIdDesc(q.trim());
        */
    }

    @Override
    @Transactional(readOnly = true)
    public CheckEntity get(Long id) {
        // Deprecated: CheckRepository 사용 불가
        throw new UnsupportedOperationException("CheckServiceImpl.get is deprecated");
        // return repo.findById(id).orElseThrow(() -> new IllegalStateException("Check not found: " + id));
    }

    @Override
    public Long create(CheckForm form) {
        // Deprecated: CheckRepository 사용 불가
        throw new UnsupportedOperationException("CheckServiceImpl.create is deprecated");
        /*
        CheckEntity e = new CheckEntity();
        applyForm(e, form);
        repo.save(e);
        return e.getId();
        */
    }

    @Override
    public void update(Long id, CheckForm form) {
        // Deprecated: CheckRepository 사용 불가
        throw new UnsupportedOperationException("CheckServiceImpl.update is deprecated");
        /*
        CheckEntity e = get(id);
        applyForm(e, form);
        repo.save(e);
        */
    }

    @Override
    public void delete(Long id) {
        // Deprecated: CheckRepository 사용 불가
        throw new UnsupportedOperationException("CheckServiceImpl.delete is deprecated");
        // repo.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public CheckForm toForm(CheckEntity e) {
        CheckForm f = new CheckForm();
        f.setName(e.getName());
        f.setTargetName(e.getTargetName());
        f.setType(e.getType() != null ? e.getType().name() : "SHELL");
        f.setIntervalSec(e.getIntervalSec());
        f.setEnabled(e.getEnabled());

        f.setScript(e.getScript());
        f.setSqlText(e.getScript());

        f.setHost(e.getHost());
        f.setTimezone(e.getTimezone());
        f.setPort(e.getPort());
        f.setSshUsername(e.getSshUsername());
        f.setSshPassword(e.getSshPassword());
        f.setSshPrivateKeyPath(e.getSshPrivateKeyPath());

        f.setDbType(e.getDbType());
        f.setDbPort(e.getDbPort());
        f.setDbName(e.getDbName());
        f.setDbUsername(e.getDbUsername());
        f.setDbPassword(e.getDbPassword());

        return f;
    }

    private void applyForm(CheckEntity e, CheckForm form) {
        e.setName(StringUtils.hasText(form.getName()) ? form.getName().trim() : null);
        e.setTargetName(StringUtils.hasText(form.getTargetName()) ? form.getTargetName().trim() : (form.getName() != null ? form.getName().trim() : ""));
        e.setHost(StringUtils.hasText(form.getHost()) ? form.getHost().trim() : null);
        e.setTimezone(StringUtils.hasText(form.getTimezone()) ? form.getTimezone().trim() : null);
        e.setIntervalSec(form.getIntervalSec() != null ? form.getIntervalSec() : 60);
        e.setEnabled(form.getEnabled() != null ? form.getEnabled() : true);

        CheckType type = CheckType.valueOf((form.getType() == null ? "SHELL" : form.getType().trim().toUpperCase()));
        e.setType(type);

        // SQL 타입이면 sqlText, SHELL 타입이면 script 사용 (폼에서 타입별로 다른 필드가 노출됨)
        String script = "SQL".equalsIgnoreCase(form.getType())
                ? (StringUtils.hasText(form.getSqlText()) ? form.getSqlText().trim() : null)
                : (StringUtils.hasText(form.getScript()) ? form.getScript().trim() : null);
        e.setScript(script);

        if ("SHELL".equalsIgnoreCase(form.getType())) {
            e.setPort(form.getPort() != null ? form.getPort() : 22);
            e.setSshUsername(StringUtils.hasText(form.getSshUsername()) ? form.getSshUsername().trim() : null);
            // SSH 패스워드: 값이 있으면 업데이트, 빈 문자열이면 기존 값 유지
            if (StringUtils.hasText(form.getSshPassword())) {
                e.setSshPassword(form.getSshPassword());
            }
            e.setSshPrivateKeyPath(StringUtils.hasText(form.getSshPrivateKeyPath()) ? form.getSshPrivateKeyPath().trim() : null);
            e.setDbType(null);
            e.setDbPort(null);
            e.setDbName(null);
            e.setDbUrl(null);
            e.setDbUsername(null);
            e.setDbPassword(null);
        } else {
            e.setPort(null);
            e.setSshUsername(null);
            e.setSshPassword(null);
            e.setSshPrivateKeyPath(null);
            e.setDbType(StringUtils.hasText(form.getDbType()) ? form.getDbType().trim().toLowerCase() : null);
            e.setDbPort(form.getDbPort());
            e.setDbName(StringUtils.hasText(form.getDbName()) ? form.getDbName().trim() : null);
            e.setDbUsername(StringUtils.hasText(form.getDbUsername()) ? form.getDbUsername().trim() : null);
            // 패스워드: 값이 있으면 업데이트, 빈 문자열이면 기존 값 유지 (password input은 변경 안하면 빈 문자열 전송)
            if (StringUtils.hasText(form.getDbPassword())) {
                e.setDbPassword(form.getDbPassword());
            }
            // form.getDbPassword()가 null이거나 빈 문자열이면 기존 값 유지 (setDbPassword 호출 안함)
            e.setDbUrl(buildDbUrl(form.getHost(), form.getDbType(), form.getDbPort(), form.getDbName()));
        }
    }

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
