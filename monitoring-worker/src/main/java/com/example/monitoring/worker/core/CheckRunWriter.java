package com.example.monitoring.worker.core;

import com.example.monitoring.common.domain.CheckEntity;
import com.example.monitoring.common.domain.CheckRunEntity;
// import com.example.monitoring.common.repo.CheckRepository;  // Deprecated
import com.example.monitoring.common.repo.CheckRunRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class CheckRunWriter {

    private final CheckRunRepository checkRunRepository;
    // Deprecated: CheckRepository는 더 이상 사용되지 않음
    // private final CheckRepository checkRepository;

    public CheckRunWriter(CheckRunRepository checkRunRepository
                          // CheckRepository checkRepository  // Deprecated
    ) {
        this.checkRunRepository = checkRunRepository;
        // this.checkRepository = checkRepository;  // Deprecated
    }

    @Transactional
    public void writeAndReschedule(CheckEntity check, RunResult result) {
        OffsetDateTime startedAt = OffsetDateTime.now().minusNanos(result.getDurationMs() * 1_000_000L);
        OffsetDateTime finishedAt = OffsetDateTime.now();

        CheckRunEntity run = new CheckRunEntity();
        run.setCheckId(check.getId());
        run.setSuccess(result.isSuccess());
        run.setDurationMs(result.getDurationMs());
        run.setOutput(result.getOutput());
        run.setErrorMessage(result.getErrorMessage());
        run.setFinishedAt(finishedAt);
        // startedAt은 엔티티 기본값이 now()라서 여기선 명시 안 해도 되지만, 맞춰주고 싶으면 setter 추가해서 넣어도 됨.

        checkRunRepository.save(run);

        // 다음 실행 시간 갱신 + 락 해제
        int intervalSec = (check.getIntervalSec() == null) ? 60 : check.getIntervalSec();
        check.setNextRunAt(OffsetDateTime.now().plusSeconds(intervalSec));
        check.setLockedUntil(null);
        check.setLockedBy(null);

        // Deprecated: CheckRepository는 더 이상 사용되지 않음
        // checkRepository.save(check);
    }

    @Transactional
    public void unlock(CheckEntity check) {
        // Deprecated: CheckRepository는 더 이상 사용되지 않음
        /*
        check.setLockedUntil(null);
        check.setLockedBy(null);
        checkRepository.save(check);
        */
    }
}