package com.example.monitoring.worker.exec;

import com.example.monitoring.common.domain.CheckEntity;
import com.example.monitoring.common.domain.CheckType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class CheckExecutor {

    private final JdbcTemplate jdbcTemplate;

    public CheckExecutor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public ExecResult execute(CheckEntity check) {
        long t0 = System.currentTimeMillis();
        try {
            if (check.getType() == CheckType.SQL) {
                // 가장 단순 버전: 결과 1행 1컬럼을 문자열로
                String out = jdbcTemplate.queryForObject(check.getScript(), String.class);
                long ms = System.currentTimeMillis() - t0;
                return ExecResult.ok(ms, out);
            }

            long ms = System.currentTimeMillis() - t0;
            return ExecResult.fail(ms, "Unsupported check type: " + check.getType());
        } catch (Exception e) {
            long ms = System.currentTimeMillis() - t0;
            return ExecResult.fail(ms, e.getMessage());
        }
    }
}