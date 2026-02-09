package com.example.monitoring.worker.support;

import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.UUID;

@Component
public class WorkerIdProvider {

    private final String workerId;

    public WorkerIdProvider() {
        this.workerId = buildWorkerId();
    }

    public String getWorkerId() {
        return workerId;
    }

    private String buildWorkerId() {
        try {
            String host = InetAddress.getLocalHost().getHostName();
            return host + "-" + UUID.randomUUID().toString().substring(0, 8);
        } catch (Exception e) {
            return "worker-" + UUID.randomUUID().toString().substring(0, 8);
        }
    }
}