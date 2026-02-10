package com.example.monitoring.worker.service;

import com.example.monitoring.common.domain.CheckEntity;
import com.example.monitoring.common.domain.CheckRunEntity;
// import com.example.monitoring.common.repo.CheckRepository;  // Deprecated
import com.example.monitoring.common.repo.CheckRunRepository;
import com.example.monitoring.worker.exec.ExecResult;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class CheckFinalizeService {

    private static final Logger log = LoggerFactory.getLogger(CheckFinalizeService.class);

    private final CheckRunRepository runRepository;
    // Deprecated: CheckRepository는 더 이상 사용되지 않음
    // private final CheckRepository checkRepository;

    public CheckFinalizeService(CheckRunRepository runRepository
                                // CheckRepository checkRepository  // Deprecated
    ) {
        this.runRepository = runRepository;
        // this.checkRepository = checkRepository;  // Deprecated
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

        // Deprecated: CheckRepository는 더 이상 사용되지 않음
        // checkRepository.save(check);
        log.warn("CheckFinalizeService.finalizeRun is deprecated. CheckRepository.save is not available.");
    }
}