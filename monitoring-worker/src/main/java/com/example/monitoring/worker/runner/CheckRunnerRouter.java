package com.example.monitoring.worker.runner;

import com.example.monitoring.common.domain.CheckType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class CheckRunnerRouter {

    private final Map<CheckType, CheckRunner> runners = new EnumMap<>(CheckType.class);

    public CheckRunnerRouter(List<CheckRunner> runnerBeans) {
        for (CheckRunner r : runnerBeans) {
            for (CheckType t : CheckType.values()) {
                if (r.supports(t)) {
                    CheckRunner prev = runners.putIfAbsent(t, r);
                    if (prev != null) {
                        throw new IllegalStateException("Duplicate CheckRunner for type=" + t
                                + " (" + prev.getClass().getName() + " vs " + r.getClass().getName() + ")");
                    }
                }
            }
        }
    }

    public CheckRunner get(CheckType type) {
        CheckRunner runner = runners.get(type);
        if (runner == null) {
            throw new UnsupportedOperationException("No CheckRunner registered for type=" + type);
        }
        return runner;
    }
}