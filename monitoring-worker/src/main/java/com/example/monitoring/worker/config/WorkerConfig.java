package com.example.monitoring.worker.config;

import com.example.monitoring.worker.WorkerProperties;
import com.example.monitoring.worker.support.WorkerIdProvider;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

/**
 * Worker 설정 초기화
 * Worker ID가 설정되지 않았으면 자동으로 고유 ID 생성
 */
@Configuration
public class WorkerConfig {

    private final WorkerProperties workerProperties;
    private final WorkerIdProvider workerIdProvider;

    public WorkerConfig(WorkerProperties workerProperties, WorkerIdProvider workerIdProvider) {
        this.workerProperties = workerProperties;
        this.workerIdProvider = workerIdProvider;
    }

    @PostConstruct
    public void initWorkerId() {
        // Worker ID가 설정되지 않았으면 자동 생성 (환경 변수나 application.yml에서 설정 가능)
        if (!StringUtils.hasText(workerProperties.getId())) {
            String autoId = workerIdProvider.getWorkerId();
            workerProperties.setId(autoId);
            System.out.println("Worker ID auto-generated: " + autoId);
        }
    }
}
