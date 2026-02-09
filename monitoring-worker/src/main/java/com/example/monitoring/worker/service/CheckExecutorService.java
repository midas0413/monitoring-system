package com.example.monitoring.worker.service;

import com.example.monitoring.common.domain.CheckEntity;
import com.example.monitoring.common.domain.CheckRunEntity;
import com.example.monitoring.common.repo.CheckRepository;
import com.example.monitoring.common.repo.CheckRunRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class CheckExecutorService {

    private final CheckRunRepository checkRunRepository;
    private final CheckRepository checkRepository;

    public CheckExecutorService(CheckRunRepository checkRunRepository,
                                CheckRepository checkRepository) {
        this.checkRunRepository = checkRunRepository;
        this.checkRepository = checkRepository;
    }

    @Transactional
    public void executeOne(CheckEntity check, ExecutionResult result) {
        // 1) check_runs insert
        CheckRunEntity run = new CheckRunEntity();
        run.setCheckId(check.getId());
        run.setSuccess(result.success());
        run.setStartedAt(result.startedAt());
        run.setFinishedAt(result.finishedAt());
        run.setDurationMs(result.durationMs());
        run.setOutput(result.output());
        run.setErrorMessage(result.errorMessage());

        checkRunRepository.save(run);

        // 2) checks update (상태 + next_run_at + unlock)
        check.setLastRunAt(result.finishedAt() != null ? result.finishedAt() : OffsetDateTime.now());
        check.setLastStatus(result.success() ? "SUCCESS" : "FAIL");

        OffsetDateTime base = OffsetDateTime.now();
        int intervalSec = (check.getIntervalSec() != null) ? check.getIntervalSec() : 60;
        check.setNextRunAt(base.plusSeconds(intervalSec));

        // unlock
        check.setLockedBy(null);
        check.setLockedUntil(null);

        checkRepository.save(check);
    }

    public record ExecutionResult(
            boolean success,
            OffsetDateTime startedAt,
            OffsetDateTime finishedAt,
            Long durationMs,
            String output,
            String errorMessage
    ) {}
}