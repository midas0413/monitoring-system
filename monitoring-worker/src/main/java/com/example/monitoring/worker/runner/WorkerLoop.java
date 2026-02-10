package com.example.monitoring.worker.runner;

import com.example.monitoring.common.domain.MonitoringRuleEntity;
import com.example.monitoring.worker.WorkerProperties;
import com.example.monitoring.worker.db.MonitoringRuleClaimDao;
import com.example.monitoring.worker.monitoring.MonitoringRuleExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 모니터링 룰 실행 루프
 * 주기적으로 실행 대상 모니터링 룰을 선점하여 실행
 */
@Component
public class WorkerLoop implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WorkerLoop.class);

    private final WorkerProperties props;
    private final MonitoringRuleClaimDao claimDao;
    private final MonitoringRuleExecutorService executorService;

    public WorkerLoop(
            WorkerProperties props,
            MonitoringRuleClaimDao claimDao,
            MonitoringRuleExecutorService executorService
    ) {
        this.props = props;
        this.claimDao = claimDao;
        this.executorService = executorService;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("WorkerLoop started. workerId={}, claimLimit={}, lockSeconds={}, sleepMs={}",
                props.getId(), props.getClaimLimit(), props.getLockSeconds(), props.getLoopSleepMs());

        while (!Thread.currentThread().isInterrupted()) {
            try {
                List<MonitoringRuleEntity> claimed = claimDao.claimDueRules(
                        props.getId(),
                        props.getClaimLimit(),
                        props.getLockSeconds()
                );

                if (claimed.isEmpty()) {
                    sleep(props.getLoopSleepMs());
                    continue;
                }

                log.debug("Claimed {} monitoring rules", claimed.size());

                for (MonitoringRuleEntity rule : claimed) {
                    try {
                        executorService.executeRule(rule, props.getId());
                    } catch (Exception e) {
                        log.error("Rule execution failed. ruleId={}, type={}",
                                rule.getId(), rule.getMonitoringType(), e);
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
