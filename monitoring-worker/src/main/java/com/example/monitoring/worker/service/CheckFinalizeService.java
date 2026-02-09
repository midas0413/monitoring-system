package com.example.monitoring.worker.service;

import com.example.monitoring.common.domain.CheckEntity;
import com.example.monitoring.common.domain.CheckRunEntity;
import com.example.monitoring.common.repo.CheckRepository;
import com.example.monitoring.common.repo.CheckRunRepository;
import com.example.monitoring.worker.exec.ExecResult;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class CheckFinalizeService {

    private final CheckRunRepository runRepository;
    private final CheckRepository checkRepository;

    public CheckFinalizeService(CheckRunRepository runRepository, CheckRepository checkRepository) {
        this.runRepository = runRepository;
        this.checkRepository = checkRepository;
    }

    @Transactional
    public void finalizeRun(CheckEntity check, OffsetDateTime startedAt, ExecResult result) {
        // 1) check_runs insert
        CheckRunEntity run = new CheckRunEntity();
        run.setCheckId(check.getId());
        run.setSuccess(result.isSuccess());
        run.setStartedAt(startedAt);
        run.setFinishedAt(OffsetDateTime.now());
        run.setDurationMs(result.getDurationMs());
        run.setOutput(result.getOutput());
        run.setErrorMessage(result.getErrorMessage());
        runRepository.save(run);

        // 2) checks update: last_run_at, last_status, next_run_at, lock 해제
        check.setLastRunAt(run.getFinishedAt());
        check.setLastStatus(result.isSuccess() ? "SUCCESS" : "FAIL");

        // next_run_at = now + interval_sec
        check.setNextRunAt(OffsetDateTime.now().plusSeconds(check.getIntervalSec()));

        // lock 해제(매우 중요)
        check.setLockedBy(null);
        check.setLockedUntil(null);

        checkRepository.save(check);
    }
}