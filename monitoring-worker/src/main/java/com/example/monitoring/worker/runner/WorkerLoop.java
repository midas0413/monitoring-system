package com.example.monitoring.worker.runner;

import com.example.monitoring.common.domain.CheckEntity;
import com.example.monitoring.worker.WorkerProperties;
import com.example.monitoring.worker.db.CheckClaimDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WorkerLoop implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WorkerLoop.class);

    private final WorkerProperties props;
    private final CheckClaimDao checkClaimDao;
    private final CheckRunnerRouter router;

    public WorkerLoop(
            WorkerProperties props,
            CheckClaimDao checkClaimDao,
            CheckRunnerRouter router
    ) {
        this.props = props;
        this.checkClaimDao = checkClaimDao;
        this.router = router;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("WorkerLoop started. workerId={}, claimLimit={}, lockSeconds={}, sleepMs={}",
                props.getId(), props.getClaimLimit(), props.getLockSeconds(), props.getLoopSleepMs());

        while (!Thread.currentThread().isInterrupted()) {
            try {
                List<CheckEntity> claimed = checkClaimDao.claimDueChecks(
                        props.getId(),
                        props.getClaimLimit(),
                        props.getLockSeconds()
                );

                if (claimed.isEmpty()) {
                    sleep(props.getLoopSleepMs());
                    continue;
                }

                for (CheckEntity check : claimed) {
                    try {
                        CheckRunner runner = router.get(check.getType());
                        runner.runOne(check, props.getId(), props.getLockSeconds());
                    } catch (Exception e) {
                        log.error("Check run failed. checkId={}, type={}",
                                check.getId(), check.getType(), e);
                    }
                }

            } catch (Exception e) {
                log.error("Claim loop error", e);
                sleep(1000);
            }
        }

        log.info("WorkerLoop stopped (interrupted).");
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
