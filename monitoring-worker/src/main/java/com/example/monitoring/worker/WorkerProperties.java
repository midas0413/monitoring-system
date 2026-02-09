package com.example.monitoring.worker;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "worker")
public class WorkerProperties {

    private String id = "worker-1";

    private int claimLimit = 10;
    private int lockSeconds = 30;
    private long loopSleepMs = 1000;

    private long sshConnectTimeoutMs = 5000;
    private long sshCommandTimeoutMs = 30000;

    private int outboxClaimLimit = 50;
    private int outboxLockSeconds = 60;
    private long outboxTickMs = 1000;

    private int outboxMaxAttemptDefault = 5;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public int getClaimLimit() { return claimLimit; }
    public void setClaimLimit(int claimLimit) { this.claimLimit = claimLimit; }

    public int getLockSeconds() { return lockSeconds; }
    public void setLockSeconds(int lockSeconds) { this.lockSeconds = lockSeconds; }

    public long getLoopSleepMs() { return loopSleepMs; }
    public void setLoopSleepMs(long loopSleepMs) { this.loopSleepMs = loopSleepMs; }

    public long getSshConnectTimeoutMs() { return sshConnectTimeoutMs; }
    public void setSshConnectTimeoutMs(long sshConnectTimeoutMs) { this.sshConnectTimeoutMs = sshConnectTimeoutMs; }

    public long getSshCommandTimeoutMs() { return sshCommandTimeoutMs; }
    public void setSshCommandTimeoutMs(long sshCommandTimeoutMs) { this.sshCommandTimeoutMs = sshCommandTimeoutMs; }

    public int getOutboxClaimLimit() { return outboxClaimLimit; }
    public void setOutboxClaimLimit(int outboxClaimLimit) { this.outboxClaimLimit = outboxClaimLimit; }

    public int getOutboxLockSeconds() { return outboxLockSeconds; }
    public void setOutboxLockSeconds(int outboxLockSeconds) { this.outboxLockSeconds = outboxLockSeconds; }

    public long getOutboxTickMs() { return outboxTickMs; }
    public void setOutboxTickMs(long outboxTickMs) { this.outboxTickMs = outboxTickMs; }

    public int getOutboxMaxAttemptDefault() { return outboxMaxAttemptDefault; }
    public void setOutboxMaxAttemptDefault(int outboxMaxAttemptDefault) { this.outboxMaxAttemptDefault = outboxMaxAttemptDefault; }
}