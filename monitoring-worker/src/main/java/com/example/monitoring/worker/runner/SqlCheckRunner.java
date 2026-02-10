package com.example.monitoring.worker.runner;

import com.example.monitoring.common.domain.CheckEntity;
import com.example.monitoring.common.domain.CheckRunEntity;
import com.example.monitoring.common.domain.CheckType;
// import com.example.monitoring.common.repo.CheckRepository;  // Deprecated
import com.example.monitoring.common.repo.CheckRunRepository;
import com.example.monitoring.worker.alert.AlertEvaluatorService;
import com.example.monitoring.worker.db.CheckConnProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.OffsetDateTime;

@Service
public class SqlCheckRunner implements CheckRunner {

    private static final Logger log = LoggerFactory.getLogger(SqlCheckRunner.class);

    // Deprecated: CheckRepository는 더 이상 사용되지 않음
    // private final CheckRepository checkRepository;
    private final CheckRunRepository checkRunRepository;
    private final CheckConnProvider checkConnProvider;
    // Deprecated: AlertEvaluatorService는 더 이상 사용되지 않음
    // private final AlertEvaluatorService alertEvaluatorService;

    public SqlCheckRunner(
            // CheckRepository checkRepository,  // Deprecated
            CheckRunRepository checkRunRepository,
            CheckConnProvider checkConnProvider
            // AlertEvaluatorService alertEvaluatorService  // Deprecated
    ) {
        // this.checkRepository = checkRepository;  // Deprecated
        this.checkRunRepository = checkRunRepository;
        this.checkConnProvider = checkConnProvider;
        // this.alertEvaluatorService = alertEvaluatorService;  // Deprecated
    }

    @Override
    public boolean supports(CheckType type) {
        return type == CheckType.SQL;
    }

    @Override
    @Transactional
    public void runOne(CheckEntity check, String workerId, int lockSeconds) {
        if (check.getType() != CheckType.SQL) {
            log.warn("Unsupported type in SqlCheckRunner. checkId={}, type={}", check.getId(), check.getType());
            unlockAndReschedule(check, "UNSUPPORTED_TYPE");
            return;
        }

        OffsetDateTime started = OffsetDateTime.now();

        boolean success = true;
        String output = null;
        String errorMessage = null;

        try {
            var jdbc = checkConnProvider.getJdbcTemplate(check);

            Object val = jdbc.queryForObject(check.getScript(), Object.class);
            output = (val == null) ? "null" : val.toString();

            if (output.length() > 5000) output = output.substring(0, 5000);

        } catch (Exception e) {
            success = false;
            
            // 더 자세한 오류 메시지 생성
            String baseMessage = StringUtils.hasText(e.getMessage()) ? e.getMessage() : e.getClass().getSimpleName();
            
            // JDBC 연결 오류인 경우 추가 정보 포함
            if (baseMessage.contains("JDBC Connection") || baseMessage.contains("Connection") || 
                baseMessage.contains("authentication") || baseMessage.contains("password")) {
                StringBuilder errorDetail = new StringBuilder(baseMessage);
                errorDetail.append(" [checkId=").append(check.getId());
                errorDetail.append(", host=").append(check.getHost());
                errorDetail.append(", dbType=").append(check.getDbType());
                errorDetail.append(", dbPort=").append(check.getDbPort());
                errorDetail.append(", dbName=").append(check.getDbName());
                errorDetail.append(", username=").append(check.getDbUsername());
                errorDetail.append(", password=").append(check.getDbPassword() != null ? "***" : "null");
                errorDetail.append("]");
                errorMessage = errorDetail.toString();
                
                log.error("SQL Check failed. checkId={}, error={}", check.getId(), baseMessage, e);
            } else {
                errorMessage = baseMessage;
                log.warn("SQL Check failed. checkId={}, error={}", check.getId(), baseMessage);
            }
            
            if (errorMessage.length() > 1000) errorMessage = errorMessage.substring(0, 1000);
        }

        OffsetDateTime finished = OffsetDateTime.now();
        long durationMs = Duration.between(started, finished).toMillis();

        CheckRunEntity run = new CheckRunEntity();
        run.setCheckId(check.getId());
        run.setSuccess(success);
        run.setStartedAt(started);
        run.setFinishedAt(finished);
        run.setDurationMs(durationMs);
        run.setOutput(output);
        run.setErrorMessage(errorMessage);
        checkRunRepository.save(run);
        // Deprecated: AlertEvaluatorService는 더 이상 사용되지 않음
        // alertEvaluatorService.evaluateAndEnqueue(run);

        // Deprecated: CheckRepository는 더 이상 사용되지 않음
        // unlockAndReschedule(check, success ? "SUCCESS" : "FAIL");
        log.warn("SqlCheckRunner.runOne is deprecated. Use MonitoringRuleExecutorService instead.");
    }

    @SuppressWarnings("unused")
    private void unlockAndReschedule(CheckEntity check, String status) {
        // Deprecated: CheckRepository는 더 이상 사용되지 않음
        /*
        OffsetDateTime now = OffsetDateTime.now();

        check.setLastRunAt(now);
        check.setLastStatus(status);

        check.setLockedBy(null);
        check.setLockedUntil(null);

        int interval = (check.getIntervalSec() == null) ? 60 : check.getIntervalSec();
        check.setNextRunAt(now.plusSeconds(interval));

        checkRepository.save(check);
        */
    }
}
