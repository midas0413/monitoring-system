package com.example.monitoring.worker.runner;

import com.example.monitoring.common.domain.CheckEntity;
import com.example.monitoring.common.domain.CheckType;

public interface CheckRunner {
    boolean supports(CheckType type);
    void runOne(CheckEntity check, String workerId, int lockSeconds);
}