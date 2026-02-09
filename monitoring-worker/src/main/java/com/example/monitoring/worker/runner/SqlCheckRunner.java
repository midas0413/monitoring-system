package com.example.monitoring.worker.runner;

import com.example.monitoring.common.domain.CheckEntity;
import com.example.monitoring.common.domain.CheckRunEntity;
import com.example.monitoring.common.domain.CheckType;
import com.example.monitoring.common.repo.CheckRepository;
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

    private final CheckRepository checkRepository;
    private final CheckRunRepository checkRunRepository;
    private final CheckConnProvider checkConnProvider;
    private final AlertEvaluatorService alertEvaluatorService;

    public SqlCheckRunner(
            CheckRepository checkRepository,
            CheckRunRepository checkRunRepository,
            CheckConnProvider checkConnProvider,
            AlertEvaluatorService alertEvaluatorService
    ) {
        this.checkRepository = checkRepository;
        this.checkRunRepository = checkRunRepository;
        this.checkConnProvider = checkConnProvider;
        this.alertEvaluatorService = alertEvaluatorService;
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
            errorMessage = StringUtils.hasText(e.getMessage()) ? e.getMessage() : e.getClass().getSimpleName();
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
        alertEvaluatorService.evaluateAndEnqueue(run);

        unlockAndReschedule(check, success ? "SUCCESS" : "FAIL");
    }

    private void unlockAndReschedule(CheckEntity check, String status) {
        OffsetDateTime now = OffsetDateTime.now();

        check.setLastRunAt(now);
        check.setLastStatus(status);

        check.setLockedBy(null);
        check.setLockedUntil(null);

        int interval = (check.getIntervalSec() == null) ? 60 : check.getIntervalSec();
        check.setNextRunAt(now.plusSeconds(interval));

        checkRepository.save(check);
    }
}
