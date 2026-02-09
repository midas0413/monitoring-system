package com.example.monitoring.worker.exec;

public class ExecResult {
    private final boolean success;
    private final long durationMs;
    private final String output;
    private final String errorMessage;

    public ExecResult(boolean success, long durationMs, String output, String errorMessage) {
        this.success = success;
        this.durationMs = durationMs;
        this.output = output;
        this.errorMessage = errorMessage;
    }

    public boolean isSuccess() { return success; }
    public long getDurationMs() { return durationMs; }
    public String getOutput() { return output; }
    public String getErrorMessage() { return errorMessage; }

    public static ExecResult ok(long durationMs, String output) {
        return new ExecResult(true, durationMs, output, null);
    }
    public static ExecResult fail(long durationMs, String errorMessage) {
        return new ExecResult(false, durationMs, null, errorMessage);
    }
}