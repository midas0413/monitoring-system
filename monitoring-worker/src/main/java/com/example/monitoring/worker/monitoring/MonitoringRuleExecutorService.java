package com.example.monitoring.worker.monitoring;

import com.example.monitoring.common.domain.MonitoringRuleEntity;
import com.example.monitoring.common.domain.ServerEntity;
import com.example.monitoring.common.repo.MonitoringRuleRepository;
import com.example.monitoring.common.repo.ServerRepository;
import com.example.monitoring.worker.db.CheckConnProvider;
import com.example.monitoring.worker.db.CheckSshProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * 모니터링 룰 실행 서비스
 * 각 모니터링 타입별로 실행 로직을 처리
 */
@Service
@Transactional
public class MonitoringRuleExecutorService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringRuleExecutorService.class);

    private final CheckConnProvider checkConnProvider;
    private final CheckSshProvider checkSshProvider;
    private final MonitoringRuleRepository ruleRepo;
    private final MonitoringRuleEvaluatorService evaluatorService;
    private final ServerRepository serverRepo;

    public MonitoringRuleExecutorService(
            CheckConnProvider checkConnProvider,
            CheckSshProvider checkSshProvider,
            MonitoringRuleRepository ruleRepo,
            MonitoringRuleEvaluatorService evaluatorService,
            ServerRepository serverRepo) {
        this.checkConnProvider = checkConnProvider;
        this.checkSshProvider = checkSshProvider;
        this.ruleRepo = ruleRepo;
        this.evaluatorService = evaluatorService;
        this.serverRepo = serverRepo;
    }

    /**
     * 모니터링 룰 실행
     */
    public void executeRule(MonitoringRuleEntity rule, String workerId) {
        log.info("Executing monitoring rule: ruleId={}, name={}, type={}, serverId={}", 
                rule.getId(), rule.getName(), rule.getMonitoringType(), rule.getServerId());
        
        if (!Boolean.TRUE.equals(rule.getEnabled())) {
            log.warn("Rule is disabled. ruleId={}", rule.getId());
            unlockRule(rule);
            return;
        }

        OffsetDateTime startedAt = OffsetDateTime.now();
        boolean success = false;
        String output = null;
        String errorMessage = null;

        try {
            ExecutionResult result = executeByType(rule);
            success = result.success();
            output = result.output();
            errorMessage = result.errorMessage();
        } catch (Exception e) {
            log.error("Rule execution error. ruleId={}, type={}", 
                    rule.getId(), rule.getMonitoringType(), e);
            success = false;
            errorMessage = StringUtils.hasText(e.getMessage()) ? e.getMessage() : e.getClass().getSimpleName();
            if (errorMessage.length() > 1000) {
                errorMessage = errorMessage.substring(0, 1000);
            }
        }

        OffsetDateTime finishedAt = OffsetDateTime.now();
        long durationMs = Duration.between(startedAt, finishedAt).toMillis();

        // 룰 평가 및 알림 발송 (evaluateAndNotify가 check_runs 저장도 처리)
        evaluatorService.evaluateAndNotify(rule, success, output, errorMessage, 
                startedAt, finishedAt, durationMs);

        // 룰 상태 업데이트 및 다음 실행 시간 설정
        updateRuleAfterExecution(rule, success, finishedAt);
    }

    /**
     * 모니터링 타입별 실행
     */
    private ExecutionResult executeByType(MonitoringRuleEntity rule) {
        return switch (rule.getMonitoringType()) {
            case DB -> executeDb(rule);
            case SHELL -> executeShell(rule);
            case LOGS -> executeLogs(rule);
            case DISK_SPACE -> executeDiskSpace(rule);
        };
    }

    /**
     * DB 모니터링 실행
     */
    private ExecutionResult executeDb(MonitoringRuleEntity rule) {
        if (!StringUtils.hasText(rule.getDbUrl())) {
            return ExecutionResult.fail(0, "DB URL is not set");
        }

        try {
            JdbcTemplate jdbc = checkConnProvider.getJdbcTemplateForRule(rule);
            Object value = jdbc.queryForObject(rule.getShellScript(), Object.class);
            String output = (value == null) ? "null" : value.toString();
            if (output.length() > 5000) {
                output = output.substring(0, 5000);
            }
            return ExecutionResult.ok(0, output);
        } catch (Exception e) {
            String errorMsg = StringUtils.hasText(e.getMessage()) ? e.getMessage() : e.getClass().getSimpleName();
            log.error("DB execution failed. ruleId={}, error={}", rule.getId(), errorMsg, e);
            return ExecutionResult.fail(0, errorMsg);
        }
    }

    /**
     * Shell 스크립트 실행
     */
    private ExecutionResult executeShell(MonitoringRuleEntity rule) {
        if (!StringUtils.hasText(rule.getShellScript())) {
            return ExecutionResult.fail(0, "Shell script is not set");
        }

        try {
            ServerEntity server = serverRepo.findById(rule.getServerId()).orElse(null);
            if (server == null) {
                return ExecutionResult.fail(0, "Server not found: " + rule.getServerId());
            }

            CheckSshProvider.ExecResult result = checkSshProvider.execForRule(rule, server);
            boolean success = (result.exitCode() == 0);
            String output = StringUtils.hasText(result.stdout()) ? result.stdout().trim() : "";
            String errorMsg = success ? null : (StringUtils.hasText(result.stderr()) ? result.stderr().trim() : "exitCode=" + result.exitCode());

            if (output.length() > 5000) {
                output = output.substring(0, 5000);
            }
            if (errorMsg != null && errorMsg.length() > 1000) {
                errorMsg = errorMsg.substring(0, 1000);
            }

            return success ? ExecutionResult.ok(0, output) : ExecutionResult.fail(0, errorMsg);
        } catch (Exception e) {
            String errorMsg = StringUtils.hasText(e.getMessage()) ? e.getMessage() : e.getClass().getSimpleName();
            log.error("Shell execution failed. ruleId={}, error={}", rule.getId(), errorMsg, e);
            return ExecutionResult.fail(0, errorMsg);
        }
    }

    /**
     * Log 파일 모니터링 실행
     * 여러 로그 파일 경로를 쉼표로 구분하여 처리
     */
    private ExecutionResult executeLogs(MonitoringRuleEntity rule) {
        if (!StringUtils.hasText(rule.getLogFilePath())) {
            return ExecutionResult.fail(0, "Log file path is not set");
        }

        try {
            ServerEntity server = serverRepo.findById(rule.getServerId()).orElse(null);
            if (server == null) {
                return ExecutionResult.fail(0, "Server not found: " + rule.getServerId());
            }

            // 쉼표로 분리된 여러 경로 처리
            String[] logPaths = rule.getLogFilePath().split(",");
            StringBuilder combinedOutput = new StringBuilder();
            boolean hasError = false;
            String lastError = null;

            for (String path : logPaths) {
                String trimmedPath = path.trim();
                if (trimmedPath.isEmpty()) {
                    continue;
                }

                try {
                    // 로그 파일에서 키워드 검색 명령어 구성
                    String command = buildLogSearchCommand(trimmedPath, rule.getIncludeKeywords(), rule.getExcludeKeywords());
                    
                    CheckSshProvider.ExecResult result = checkSshProvider.execForRule(
                            rule, server, command, 5000, 30000);
                    
                    int exitCode = result.exitCode();
                    String stdout = StringUtils.hasText(result.stdout()) ? result.stdout().trim() : "";
                    String stderr = StringUtils.hasText(result.stderr()) ? result.stderr().trim() : "";
                    
                    // stderr에 실제 에러 메시지가 있는지 확인
                    boolean hasRealError = StringUtils.hasText(stderr) && 
                            (stderr.contains("No such file") || 
                             stderr.contains("Permission denied") ||
                             stderr.contains("cannot open") ||
                             stderr.contains("cannot read") ||
                             stderr.contains("No such file or directory") ||
                             stderr.contains("Access denied"));
                    
                    if (hasRealError) {
                        // 실제 파일 읽기 오류
                        hasError = true;
                        lastError = "Failed to read " + trimmedPath + ": " + stderr;
                        log.warn("Log file read failed. ruleId={}, path={}, error={}", 
                                rule.getId(), trimmedPath, stderr);
                    } else if (exitCode == 0 || StringUtils.hasText(stdout)) {
                        // 성공 또는 grep이 매칭을 찾은 경우
                        if (StringUtils.hasText(stdout)) {
                            if (combinedOutput.length() > 0) {
                                combinedOutput.append("\n---\n");
                            }
                            combinedOutput.append("[").append(trimmedPath).append("]\n");
                            combinedOutput.append(stdout);
                        }
                    } else if (exitCode == 1 && !StringUtils.hasText(stderr)) {
                        // grep이 매칭을 못 찾아서 exit code 1이지만 stderr가 없으면 정상 (매칭 없음)
                        // 이 경우는 output에 추가하지 않고 넘어감 (매칭 없음 = 정상)
                        log.debug("No matching lines found in log file. ruleId={}, path={}", 
                                rule.getId(), trimmedPath);
                    } else {
                        // 기타 오류
                        hasError = true;
                        String errorMsg = StringUtils.hasText(stderr) ? stderr : "exitCode=" + exitCode;
                        lastError = "Failed to read " + trimmedPath + ": " + errorMsg;
                        log.warn("Log file read failed. ruleId={}, path={}, error={}", 
                                rule.getId(), trimmedPath, errorMsg);
                    }
                } catch (Exception e) {
                    hasError = true;
                    lastError = "Error processing " + trimmedPath + ": " + e.getMessage();
                    log.error("Error processing log file. ruleId={}, path={}", 
                            rule.getId(), trimmedPath, e);
                }
            }

            String finalOutput = combinedOutput.toString();
            if (finalOutput.length() > 5000) {
                finalOutput = finalOutput.substring(0, 5000);
            }

            // 에러가 있지만 일부 파일은 성공한 경우
            if (hasError && finalOutput.length() > 0) {
                finalOutput += "\n[WARNING] " + lastError;
            }

            if (finalOutput.isEmpty()) {
                return ExecutionResult.ok(0, "No matching log entries found");
            }

            return ExecutionResult.ok(0, finalOutput);
        } catch (Exception e) {
            String errorMsg = StringUtils.hasText(e.getMessage()) ? e.getMessage() : e.getClass().getSimpleName();
            log.error("Log monitoring failed. ruleId={}, error={}", rule.getId(), errorMsg, e);
            return ExecutionResult.fail(0, errorMsg);
        }
    }

    /**
     * 로그 파일 검색 명령어 구성
     */
    private String buildLogSearchCommand(String logPath, String includeKeywords, String excludeKeywords) {
        StringBuilder command = new StringBuilder();
        
        // includeKeywords가 있으면 grep으로 필터링, 없으면 tail로 최근 로그만 읽기
        if (StringUtils.hasText(includeKeywords)) {
            // 키워드를 OR로 연결 (예: ERROR|FAIL|CRITICAL)
            String[] keywords = includeKeywords.split(",");
            StringBuilder pattern = new StringBuilder();
            for (int i = 0; i < keywords.length; i++) {
                if (i > 0) pattern.append("|");
                pattern.append(keywords[i].trim());
            }
            
            // grep으로 키워드 검색 (최근 1000줄만)
            command.append("tail -n 1000 ").append(logPath)
                   .append(" | grep -E '").append(pattern).append("'");
        } else {
            // 키워드가 없으면 최근 100줄만 읽기
            command.append("tail -n 100 ").append(logPath);
        }
        
        // excludeKeywords가 있으면 추가 필터링
        if (StringUtils.hasText(excludeKeywords)) {
            String[] excludeKeys = excludeKeywords.split(",");
            for (String excludeKey : excludeKeys) {
                String trimmed = excludeKey.trim();
                if (!trimmed.isEmpty()) {
                    command.append(" | grep -v '").append(trimmed).append("'");
                }
            }
        }
        
        return command.toString();
    }

    /**
     * 디스크 공간 모니터링 실행
     */
    private ExecutionResult executeDiskSpace(MonitoringRuleEntity rule) {
        try {
            ServerEntity server = serverRepo.findById(rule.getServerId()).orElse(null);
            if (server == null) {
                return ExecutionResult.fail(0, "Server not found: " + rule.getServerId());
            }

            // df -h 명령어 실행
            String command;
            if (StringUtils.hasText(rule.getDiskPath())) {
                // 특정 경로의 디스크 사용률 확인
                command = "df -h " + rule.getDiskPath().trim();
            } else {
                // 전체 마운트 포인트 확인
                command = "df -h";
            }

            CheckSshProvider.ExecResult result = checkSshProvider.execForRule(rule, server, command, 5000, 30000);
            
            if (result.exitCode() != 0) {
                String errorMsg = StringUtils.hasText(result.stderr()) ? result.stderr().trim() : 
                                 "exitCode=" + result.exitCode();
                log.error("Disk space check failed. ruleId={}, error={}", rule.getId(), errorMsg);
                return ExecutionResult.fail(0, errorMsg);
            }

            String stdout = StringUtils.hasText(result.stdout()) ? result.stdout().trim() : "";
            if (!StringUtils.hasText(stdout)) {
                return ExecutionResult.fail(0, "디스크 정보를 가져올 수 없습니다.");
            }

            // df 출력 파싱 및 사용률 추출
            String[] lines = stdout.split("\n");
            if (lines.length < 2) {
                return ExecutionResult.fail(0, "디스크 정보가 올바르지 않습니다.");
            }

            StringBuilder output = new StringBuilder();
            boolean hasThresholdExceeded = false;
            Double threshold = rule.getThresholdNum();
            Double maxUsePercent = null;

            // path가 지정되지 않은 경우: 전체 마운트 포인트 확인 후 임계값을 넘는 것만 출력
            if (!StringUtils.hasText(rule.getDiskPath())) {
                // 데이터 라인 처리 (헤더 제외)
                for (int i = 1; i < lines.length; i++) {
                    if (!StringUtils.hasText(lines[i])) {
                        continue;
                    }

                    // 공백으로 분리 (여러 공백도 하나로 처리)
                    String[] parts = lines[i].trim().split("\\s+");
                    if (parts.length < 5) {
                        continue;
                    }

                    // Filesystem, Size, Used, Avail, Use%, Mounted on
                    String usePercentStr = parts[4].replace("%", ""); // "53%" -> "53"

                    try {
                        Double usePercent = Double.parseDouble(usePercentStr);
                        
                        // 최대 사용률 추적
                        if (maxUsePercent == null || usePercent > maxUsePercent) {
                            maxUsePercent = usePercent;
                        }
                        
                        // 임계값이 설정된 경우, 임계값을 넘는 것만 출력
                        if (threshold != null && usePercent > threshold) {
                            if (output.length() == 0) {
                                // 첫 번째 항목이면 헤더 추가
                                output.append(lines[0]).append("\n");
                            }
                            output.append(lines[i]).append("\n");
                            hasThresholdExceeded = true;
                        }
                    } catch (NumberFormatException e) {
                        // 사용률 파싱 실패 시 해당 라인은 건너뛰기
                        log.warn("Failed to parse disk usage percentage. line={}, ruleId={}", lines[i], rule.getId());
                    }
                }

                String finalOutput = output.toString().trim();
                
                // 임계값을 넘는 것이 없는 경우
                if (threshold != null && !hasThresholdExceeded) {
                    return ExecutionResult.ok(0, "모든 마운트 포인트의 사용률이 임계값(" + threshold + "%) 이하입니다.");
                }
                
                // 임계값이 설정되지 않은 경우, 최대 사용률만 출력
                if (threshold == null && maxUsePercent != null) {
                    return ExecutionResult.ok(0, String.valueOf(maxUsePercent));
                }
                
                // 임계값을 넘는 것이 있는 경우, 사용률만 출력 (evaluator에서 추출하기 쉽도록)
                if (hasThresholdExceeded && maxUsePercent != null) {
                    // 사용률을 명확히 추출할 수 있도록 출력 형식 조정
                    // 전체 정보도 포함하되, 사용률이 명확히 보이도록
                    return ExecutionResult.ok(0, finalOutput + "\n\n최대 사용률: " + maxUsePercent + "%");
                }
                
                return ExecutionResult.ok(0, finalOutput);
            } else {
                // path가 지정된 경우: 해당 경로만 확인하고 전체 정보 출력
                output.append(lines[0]).append("\n");
                
                for (int i = 1; i < lines.length; i++) {
                    if (!StringUtils.hasText(lines[i])) {
                        continue;
                    }

                    String[] parts = lines[i].trim().split("\\s+");
                    if (parts.length < 5) {
                        continue;
                    }

                    String usePercentStr = parts[4].replace("%", "");

                    try {
                        Double usePercent = Double.parseDouble(usePercentStr);
                        output.append(lines[i]).append("\n");
                        if (threshold != null && usePercent > threshold) {
                            hasThresholdExceeded = true;
                        }
                        if (maxUsePercent == null || usePercent > maxUsePercent) {
                            maxUsePercent = usePercent;
                        }
                    } catch (NumberFormatException e) {
                        log.warn("Failed to parse disk usage percentage. line={}, ruleId={}", lines[i], rule.getId());
                    }
                }

                String finalOutput = output.toString().trim();
                if (finalOutput.length() > 5000) {
                    finalOutput = finalOutput.substring(0, 5000);
                }

                return ExecutionResult.ok(0, finalOutput);
            }

        } catch (Exception e) {
            String errorMsg = StringUtils.hasText(e.getMessage()) ? e.getMessage() : e.getClass().getSimpleName();
            log.error("Disk space execution failed. ruleId={}, error={}", rule.getId(), errorMsg, e);
            return ExecutionResult.fail(0, errorMsg);
        }
    }

    /**
     * 실행 후 룰 상태 업데이트
     */
    private void updateRuleAfterExecution(MonitoringRuleEntity rule, boolean success, OffsetDateTime finishedAt) {
        rule.setLastRunAt(finishedAt);
        rule.setLastStatus(success ? "SUCCESS" : "FAIL");

        // 다음 실행 시간 설정
        int intervalSec = (rule.getIntervalSec() != null) ? rule.getIntervalSec() : 60;
        rule.setNextRunAt(finishedAt.plusSeconds(intervalSec));

        // 락 해제
        rule.setLockedBy(null);
        rule.setLockedUntil(null);

        ruleRepo.save(rule);
    }

    /**
     * 룰 락 해제
     */
    private void unlockRule(MonitoringRuleEntity rule) {
        rule.setLockedBy(null);
        rule.setLockedUntil(null);
        ruleRepo.save(rule);
    }

    /**
     * 실행 결과
     */
    private record ExecutionResult(boolean success, long durationMs, String output, String errorMessage) {
        static ExecutionResult ok(long durationMs, String output) {
            return new ExecutionResult(true, durationMs, output, null);
        }

        static ExecutionResult fail(long durationMs, String errorMessage) {
            return new ExecutionResult(false, durationMs, null, errorMessage);
        }
    }
}
