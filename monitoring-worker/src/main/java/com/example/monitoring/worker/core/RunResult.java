package com.example.monitoring.worker.core;

public class RunResult {
    private final boolean success;
    private final long durationMs;
    private final String output;
    private final String errorMessage;

    public RunResult(boolean success, long durationMs, String output, String errorMessage) {
        this.success = success;
        this.durationMs = durationMs;
        this.output = output;
        this.errorMessage = errorMessage;
    }

    public boolean isSuccess() { return success; }
    public long getDurationMs() { return durationMs; }
    public String getOutput() { return output; }
    public String getErrorMessage() { return errorMessage; }

    public static RunResult ok(long durationMs, String output) {
        return new RunResult(true, durationMs, output, null);
    }

    public static RunResult fail(long durationMs, String errorMessage) {
        return new RunResult(false, durationMs, null, errorMessage);
    }
}