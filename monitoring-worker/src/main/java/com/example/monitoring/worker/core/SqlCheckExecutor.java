package com.example.monitoring.worker.core;

import com.example.monitoring.common.domain.CheckEntity;
import com.example.monitoring.common.domain.CheckType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class SqlCheckExecutor {

    private final JdbcTemplate jdbcTemplate;

    public SqlCheckExecutor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public RunResult execute(CheckEntity check) {
        if (check.getType() != CheckType.SQL) {
            return RunResult.fail(0, "Unsupported type for SqlCheckExecutor: " + check.getType());
        }

        long start = System.currentTimeMillis();
        try {
            // MVP: 결과를 1줄 문자열로만 받자
            Object value = jdbcTemplate.queryForObject(check.getScript(), Object.class);
            long duration = System.currentTimeMillis() - start;

            String out = (value == null) ? "null" : value.toString();
            return RunResult.ok(duration, out);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            return RunResult.fail(duration, e.getMessage());
        }
    }
}