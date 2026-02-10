package com.example.monitoring.worker.monitoring;

import com.example.monitoring.common.domain.*;
import com.example.monitoring.common.repo.*;
import com.example.monitoring.worker.db.CheckConnProvider;
import com.example.monitoring.worker.db.CheckSshProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * 모니터링 룰 실행 서비스
 * 각 모니터링 타입별로 실행 로직을 처리
 */
@Service
@Transactional
public class MonitoringRuleExecutorService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringRuleExecutorService.class);

    private final CheckConnProvider checkConnProvider;
    private final CheckSshProvider checkSshProvider;
    private final MonitoringRuleRepository ruleRepo;
    private final MonitoringRuleEvaluatorService evaluatorService;
    private final ServerRepository serverRepo;

    public MonitoringRuleExecutorService(
            CheckConnProvider checkConnProvider,
            CheckSshProvider checkSshProvider,
            MonitoringRuleRepository ruleRepo,
            MonitoringRuleEvaluatorService evaluatorService,
            ServerRepository serverRepo) {
        this.checkConnProvider = checkConnProvider;
        this.checkSshProvider = checkSshProvider;
        this.ruleRepo = ruleRepo;
        this.evaluatorService = evaluatorService;
        this.serverRepo = serverRepo;
    }

    /**
     * 모니터링 룰 실행
     */
    public void executeRule(MonitoringRuleEntity rule, String workerId) {
        if (!Boolean.TRUE.equals(rule.getEnabled())) {
            log.warn("Rule is disabled. ruleId={}", rule.getId());
            unlockRule(rule);
            return;
        }

        OffsetDateTime startedAt = OffsetDateTime.now();
        boolean success = false;
        String output = null;
        String errorMessage = null;

        try {
            ExecutionResult result = executeByType(rule);
            success = result.success();
            output = result.output();
            errorMessage = result.errorMessage();
        } catch (Exception e) {
            log.error("Rule execution error. ruleId={}, type={}", 
                    rule.getId(), rule.getMonitoringType(), e);
            success = false;
            errorMessage = StringUtils.hasText(e.getMessage()) ? e.getMessage() : e.getClass().getSimpleName();
            if (errorMessage.length() > 1000) {
                errorMessage = errorMessage.substring(0, 1000);
            }
        }

        OffsetDateTime finishedAt = OffsetDateTime.now();
        long durationMs = Duration.between(startedAt, finishedAt).toMillis();

        // 룰 평가 및 알림 발송 (evaluateAndNotify가 check_runs 저장도 처리)
        evaluatorService.evaluateAndNotify(rule, success, output, errorMessage, 
                startedAt, finishedAt, durationMs);

        // 룰 상태 업데이트 및 다음 실행 시간 설정
        updateRuleAfterExecution(rule, success, finishedAt);
    }

    /**
     * 모니터링 타입별 실행
     */
    private ExecutionResult executeByType(MonitoringRuleEntity rule) {
        return switch (rule.getMonitoringType()) {
            case DB -> executeDb(rule);
            case SHELL -> executeShell(rule);
            case LOGS -> executeLogs(rule);
            case DISK_SPACE -> executeDiskSpace(rule);
        };
    }

    /**
     * DB 모니터링 실행
     */
    private ExecutionResult executeDb(MonitoringRuleEntity rule) {
        if (!StringUtils.hasText(rule.getDbUrl())) {
            return ExecutionResult.fail(0, "DB URL is not set");
        }

        try {
            JdbcTemplate jdbc = checkConnProvider.getJdbcTemplateForRule(rule);
            Object value = jdbc.queryForObject(rule.getShellScript(), Object.class);
            String output = (value == null) ? "null" : value.toString();
            if (output.length() > 5000) {
                output = output.substring(0, 5000);
            }
            return ExecutionResult.ok(0, output);
        } catch (Exception e) {
            String errorMsg = StringUtils.hasText(e.getMessage()) ? e.getMessage() : e.getClass().getSimpleName();
            log.error("DB execution failed. ruleId={}, error={}", rule.getId(), errorMsg, e);
            return ExecutionResult.fail(0, errorMsg);
        }
    }

    /**
     * Shell 스크립트 실행
     */
    private ExecutionResult executeShell(MonitoringRuleEntity rule) {
        if (!StringUtils.hasText(rule.getShellScript())) {
            return ExecutionResult.fail(0, "Shell script is not set");
        }

        try {
            ServerEntity server = serverRepo.findById(rule.getServerId()).orElse(null);
            if (server == null) {
                return ExecutionResult.fail(0, "Server not found: " + rule.getServerId());
            }

            CheckSshProvider.ExecResult result = checkSshProvider.execForRule(rule, server);
            boolean success = (result.exitCode() == 0);
            String output = StringUtils.hasText(result.stdout()) ? result.stdout().trim() : "";
            String errorMsg = success ? null : (StringUtils.hasText(result.stderr()) ? result.stderr().trim() : "exitCode=" + result.exitCode());

            if (output.length() > 5000) {
                output = output.substring(0, 5000);
            }
            if (errorMsg != null && errorMsg.length() > 1000) {
                errorMsg = errorMsg.substring(0, 1000);
            }

            return success ? ExecutionResult.ok(0, output) : ExecutionResult.fail(0, errorMsg);
        } catch (Exception e) {
            String errorMsg = StringUtils.hasText(e.getMessage()) ? e.getMessage() : e.getClass().getSimpleName();
            log.error("Shell execution failed. ruleId={}, error={}", rule.getId(), errorMsg, e);
            return ExecutionResult.fail(0, errorMsg);
        }
    }

    /**
     * Log 파일 모니터링 실행
     */
    private ExecutionResult executeLogs(MonitoringRuleEntity rule) {
        // TODO: Log 파일 모니터링 로직 구현
        return ExecutionResult.fail(0, "LOGS monitoring not implemented yet");
    }

    /**
     * 디스크 공간 모니터링 실행
     */
    private ExecutionResult executeDiskSpace(MonitoringRuleEntity rule) {
        // TODO: 디스크 공간 모니터링 로직 구현
        return ExecutionResult.fail(0, "DISK_SPACE monitoring not implemented yet");
    }

    /**
     * 실행 후 룰 상태 업데이트
     */
    private void updateRuleAfterExecution(MonitoringRuleEntity rule, boolean success, OffsetDateTime finishedAt) {
        rule.setLastRunAt(finishedAt);
        rule.setLastStatus(success ? "SUCCESS" : "FAIL");

        // 다음 실행 시간 설정
        int intervalSec = (rule.getIntervalSec() != null) ? rule.getIntervalSec() : 60;
        rule.setNextRunAt(finishedAt.plusSeconds(intervalSec));

        // 락 해제
        rule.setLockedBy(null);
        rule.setLockedUntil(null);

        ruleRepo.save(rule);
    }

    /**
     * 룰 락 해제
     */
    private void unlockRule(MonitoringRuleEntity rule) {
        rule.setLockedBy(null);
        rule.setLockedUntil(null);
        ruleRepo.save(rule);
    }

    /**
     * 실행 결과
     */
    private record ExecutionResult(boolean success, long durationMs, String output, String errorMessage) {
        static ExecutionResult ok(long durationMs, String output) {
            return new ExecutionResult(true, durationMs, output, null);
        }

        static ExecutionResult fail(long durationMs, String errorMessage) {
            return new ExecutionResult(false, durationMs, null, errorMessage);
        }
    }
}
