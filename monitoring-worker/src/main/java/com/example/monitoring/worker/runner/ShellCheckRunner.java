package com.example.monitoring.worker.runner;

import com.example.monitoring.common.domain.CheckEntity;
import com.example.monitoring.common.domain.CheckRunEntity;
import com.example.monitoring.common.domain.CheckType;
import com.example.monitoring.common.repo.CheckRepository;
import com.example.monitoring.common.repo.CheckRunRepository;
import com.example.monitoring.worker.WorkerProperties;
import com.example.monitoring.worker.alert.AlertEvaluatorService;
import com.example.monitoring.worker.db.CheckSshProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.OffsetDateTime;

@Service
public class ShellCheckRunner implements CheckRunner {

    private static final Logger log = LoggerFactory.getLogger(ShellCheckRunner.class);

    private final WorkerProperties props;
    private final CheckSshProvider sshProvider;
    private final CheckRunRepository checkRunRepository;
    private final CheckRepository checkRepository;
    private final AlertEvaluatorService alertEvaluatorService;

    public ShellCheckRunner(
            WorkerProperties props,
            CheckSshProvider sshProvider,
            CheckRunRepository checkRunRepository,
            CheckRepository checkRepository,
            AlertEvaluatorService alertEvaluatorService
    ) {
        this.props = props;
        this.sshProvider = sshProvider;
        this.checkRunRepository = checkRunRepository;
        this.checkRepository = checkRepository;
        this.alertEvaluatorService = alertEvaluatorService;
    }

    @Override
    public boolean supports(CheckType type) {
        return type == CheckType.SHELL;
    }

    @Override
    @Transactional
    public void runOne(CheckEntity check, String workerId, int lockSeconds) {
        if (check.getType() != CheckType.SHELL) {
            log.warn("Unsupported type in ShellCheckRunner. checkId={}, type={}", check.getId(), check.getType());
            unlockAndReschedule(check, "UNSUPPORTED_TYPE");
            return;
        }

        OffsetDateTime started = OffsetDateTime.now();

        boolean success = true;
        String output = null;
        String errorMessage = null;

        try {
            if (!StringUtils.hasText(check.getScript())) {
                throw new IllegalStateException("Empty script");
            }

            CheckSshProvider.ExecResult r = sshProvider.exec(
                    check,
                    check.getScript(),
                    props.getSshConnectTimeoutMs(),
                    props.getSshCommandTimeoutMs()
            );

            success = (r.exitCode() == 0);

            String stdout = StringUtils.hasText(r.stdout()) ? r.stdout().trim() : "";
            String stderr = StringUtils.hasText(r.stderr()) ? r.stderr().trim() : "";

            // 저장 정책
            output = stdout;
            if (!success) {
                errorMessage = StringUtils.hasText(stderr) ? stderr : ("exitCode=" + r.exitCode());
            }

            if (output != null && output.length() > 5000) output = output.substring(0, 5000);
            if (errorMessage != null && errorMessage.length() > 1000) errorMessage = errorMessage.substring(0, 1000);

        } catch (Exception e) {
            success = false;
            errorMessage = StringUtils.hasText(e.getMessage()) ? e.getMessage() : e.getClass().getSimpleName();
            if (errorMessage.length() > 1000) errorMessage = errorMessage.substring(0, 1000);
        }

        OffsetDateTime finished = OffsetDateTime.now();
        long durationMs = Duration.between(started, finished).toMillis();

        // 1) check_runs 저장
        CheckRunEntity run = new CheckRunEntity();
        run.setCheckId(check.getId());
        run.setSuccess(success);

        // ✅ started_at 정확히 세팅
        run.setStartedAt(started);

        run.setFinishedAt(finished);
        run.setDurationMs(durationMs);
        run.setOutput(output);
        run.setErrorMessage(errorMessage);

        run = checkRunRepository.save(run);

        // ✅ 2) 알림 평가 + outbox enqueue (임계치/실패 조건 등)
        alertEvaluatorService.evaluateAndEnqueue(run);

        // 3) checks 갱신 + unlock + next_run_at
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