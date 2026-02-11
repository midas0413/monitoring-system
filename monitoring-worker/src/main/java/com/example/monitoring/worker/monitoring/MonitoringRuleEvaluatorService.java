package com.example.monitoring.worker.monitoring;

import com.example.monitoring.common.domain.*;
import com.example.monitoring.common.repo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 모니터링 룰 평가 및 알림 발송
 * 룰 실행 후 규칙과 비교하여 해당되는 것만 check_runs에 등록 후 바로 알림
 */
@Service
@Transactional
public class MonitoringRuleEvaluatorService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringRuleEvaluatorService.class);

    private final MonitoringRuleRepository ruleRepo;
    private final CheckRunRepository checkRunRepo;
    private final NotificationOutboxRepository outboxRepo;
    private final AlertRuleRecipientLinkRepository linkRepo;
    private final ServerRepository serverRepo;
    private final TimezoneCodeRepository timezoneRepo;

    private static final DateTimeFormatter DEFAULT_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DEFAULT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DEFAULT_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    public MonitoringRuleEvaluatorService(
            MonitoringRuleRepository ruleRepo,
            CheckRunRepository checkRunRepo,
            NotificationOutboxRepository outboxRepo,
            AlertRuleRecipientLinkRepository linkRepo,
            ServerRepository serverRepo,
            TimezoneCodeRepository timezoneRepo) {
        this.ruleRepo = ruleRepo;
        this.checkRunRepo = checkRunRepo;
        this.outboxRepo = outboxRepo;
        this.linkRepo = linkRepo;
        this.serverRepo = serverRepo;
        this.timezoneRepo = timezoneRepo;
    }

    /**
     * 모니터링 룰 실행 결과 평가 및 알림 발송
     * 룰 실행 후 규칙과 비교하여 해당되는 것만 check_runs에 등록 후 바로 알림
     */
    @Transactional
    public void evaluateAndNotify(MonitoringRuleEntity rule, boolean success, String output, 
                                  String errorMessage, OffsetDateTime startedAt, 
                                  OffsetDateTime finishedAt, long durationMs) {
        OffsetDateTime now = OffsetDateTime.now();

        // 1. 규칙 평가
        if (!evaluateRule(rule, success, output)) {
            log.debug("Rule evaluation failed: ruleId={}, success={}", rule.getId(), success);
            return;
        }

        // 2. LOGS 타입인 경우 출력값 길이 체크 (직전 알림 발송 시 출력값 길이와 같으면 알림 발송하지 않음)
        if (rule.getMonitoringType() == MonitoringType.LOGS) {
            int currentOutputLength = (output != null) ? output.length() : 0;
            Integer lastNotificationOutputLength = rule.getLastNotificationOutputLength();
            
            if (lastNotificationOutputLength != null && currentOutputLength == lastNotificationOutputLength) {
                log.debug("Output length same as last notification: ruleId={}, length={}", 
                        rule.getId(), currentOutputLength);
                return;
            }
        }

        // 3. Cooldown 체크 (동일한 rule_id와 output이 cooldown 시간 내에 있는지 확인)
        if (!isCooldownOk(rule, output, now)) {
            log.debug("Cooldown period not passed: ruleId={}, output={}", rule.getId(), 
                    output != null ? output.substring(0, Math.min(50, output.length())) : "null");
            return;
        }

        // 4. check_runs에 등록
        CheckRunEntity run = new CheckRunEntity();
        run.setMonitoringRuleId(rule.getId());
        run.setSuccess(success);
        run.setStartedAt(startedAt);
        run.setFinishedAt(finishedAt);
        run.setDurationMs(durationMs);
        run.setOutput(output);
        run.setErrorMessage(errorMessage);
        run = checkRunRepo.save(run);

        log.info("Check run created: ruleId={}, runId={}, success={}", rule.getId(), run.getId(), success);

        // 5. 알림 발송
        rule.setLastFiredAt(now);
        // LOGS 타입인 경우 출력값 길이 저장
        if (rule.getMonitoringType() == MonitoringType.LOGS) {
            int currentOutputLength = (output != null) ? output.length() : 0;
            rule.setLastNotificationOutputLength(currentOutputLength);
        }
        ruleRepo.save(rule);

        enqueueNotifications(rule, run, now);
    }

    /**
     * 규칙 평가
     */
    private boolean evaluateRule(MonitoringRuleEntity rule, boolean success, String output) {
        String outputStr = (output == null) ? "" : output;

        return switch (rule.getAlertOperator()) {
            case RUN_FAILED -> !success;
            case OUTPUT_NUM_GT -> {
                Double th = rule.getThresholdNum();
                Double val = extractOutputNum(outputStr);
                yield (th != null && val != null && val > th);
            }
            case OUTPUT_NUM_LT -> {
                Double th = rule.getThresholdNum();
                Double val = extractOutputNum(outputStr);
                yield (th != null && val != null && val < th);
            }
            case OUTPUT_LEN_GT -> {
                Integer th = rule.getThresholdLen();
                int len = outputStr.length();
                yield (th != null && len > th);
            }
            case OUTPUT_LEN_LT -> {
                Integer th = rule.getThresholdLen();
                int len = outputStr.length();
                yield (th != null && len < th);
            }
            case OUTPUT_CONTAINS -> {
                String p = rule.getPattern();
                yield StringUtils.hasText(p) && outputStr.contains(p);
            }
            case OUTPUT_NOT_CONTAINS -> {
                String p = rule.getPattern();
                yield StringUtils.hasText(p) && !outputStr.contains(p);
            }
            case OUTPUT_MATCHES -> {
                String regex = rule.getPattern();
                if (!StringUtils.hasText(regex)) yield false;
                try {
                    Pattern pattern = Pattern.compile(regex);
                    Matcher matcher = pattern.matcher(outputStr);
                    yield matcher.find();
                } catch (Exception e) {
                    log.warn("Invalid regex pattern: {}", regex, e);
                    yield false;
                }
            }
        };
    }

    /**
     * Cooldown 체크
     * cooldown 시간 내에 동일한 알림이 있을 경우 (rule_id, output이 같은 경우) 알림을 통보하지 않음
     */
    private boolean isCooldownOk(MonitoringRuleEntity rule, String output, OffsetDateTime now) {
        Integer cooldownSec = rule.getCooldownSec();
        if (cooldownSec == null || cooldownSec <= 0) {
            return true;
        }

        OffsetDateTime lastFired = rule.getLastFiredAt();
        if (lastFired == null) {
            return true;
        }

        // Cooldown 시간이 지났는지 확인
        OffsetDateTime cooldownEnd = lastFired.plusSeconds(cooldownSec);
        if (now.isBefore(cooldownEnd)) {
            return false;
        }

        // 동일한 output이 cooldown 시간 내에 있는지 확인
        OffsetDateTime checkStart = now.minusSeconds(cooldownSec);
        List<CheckRunEntity> recentRuns = checkRunRepo.findByMonitoringRuleIdAndStartedAtAfter(
                rule.getId(), checkStart);
        
        for (CheckRunEntity run : recentRuns) {
            if (Objects.equals(run.getOutput(), output)) {
                log.debug("Duplicate output found in cooldown period: ruleId={}, runId={}", 
                        rule.getId(), run.getId());
                return false;
            }
        }

        return true;
    }

    /**
     * 알림 큐에 추가
     */
    private void enqueueNotifications(MonitoringRuleEntity rule, CheckRunEntity run, OffsetDateTime now) {
        // TODO: 수신자 목록 가져오기 (기존 alert_rule_recipient_links 사용)
        // 현재는 간단히 구현
        List<AlertRuleRecipientLinkEntity> links = linkRepo.findByRuleIdAndEnabledTrue(rule.getId());
        
        if (links.isEmpty()) {
            log.warn("No recipients found for rule: ruleId={}", rule.getId());
            return;
        }

        String message = renderMessage(rule, run);
        String channels = rule.getChannels();

        for (AlertRuleRecipientLinkEntity link : links) {
            AlertRecipientEntity recipient = link.getRecipient();
            if (recipient == null || !recipient.getEnabled()) {
                continue;
            }

            // Rule에 설정된 채널 우선, 없으면 수신자 채널 사용
            String finalChannels = StringUtils.hasText(channels) ? channels : recipient.getChannels();
            if (!StringUtils.hasText(finalChannels)) {
                continue;
            }

            String[] channelArray = finalChannels.split(",");
            for (String channelStr : channelArray) {
                String ch = channelStr.trim().toUpperCase();
                NotificationChannel channel = parseChannel(ch);
                if (channel == null) continue;

                String toAddr = getRecipientAddress(recipient, channel);
                if (!StringUtils.hasText(toAddr)) continue;

                NotificationOutboxEntity notification = new NotificationOutboxEntity();
                notification.setStatus(NotificationStatus.PENDING);
                notification.setChannel(channel);
                notification.setMonitoringRuleId(rule.getId());
                notification.setCheckRunId(run.getId());
                notification.setToAddr(toAddr);
                notification.setTitle("Monitoring Alert: " + rule.getName());
                notification.setBody(message);
                notification.setCreatedAt(now);
                notification.setNextAttemptAt(now);

                outboxRepo.save(notification);
                log.info("Notification enqueued: ruleId={}, runId={}, channel={}, to={}", 
                        rule.getId(), run.getId(), channel, toAddr);
            }
        }
    }

    private String renderMessage(MonitoringRuleEntity rule, CheckRunEntity run) {
        String template = rule.getMessageTemplate();
        if (template == null) {
            template = "${ruleName}에 모니터링 알림이 발생하였습니다.";
        }

        // 서버 정보 조회
        ServerEntity server = null;
        String serverName = "";
        String serverHost = "";
        String serverTimezone = null;
        ZoneId serverZoneId = null;
        if (rule.getServerId() != null) {
            Optional<ServerEntity> serverOpt = serverRepo.findById(rule.getServerId());
            if (serverOpt.isPresent()) {
                server = serverOpt.get();
                serverName = server.getName() != null ? server.getName() : "";
                serverHost = server.getHost() != null ? server.getHost() : "";
                serverTimezone = server.getTimezone();
                if (serverTimezone != null && !serverTimezone.isBlank()) {
                    try {
                        serverZoneId = ZoneId.of(serverTimezone);
                    } catch (Exception e) {
                        log.warn("Invalid timezone: {}", serverTimezone, e);
                    }
                }
            }
        }

        // 출력값 추출
        Double outputNum = extractOutputNum(run.getOutput());
        int outputLen = (run.getOutput() != null) ? run.getOutput().length() : 0;
        String threshold = (rule.getThresholdNum() != null) ? String.valueOf(rule.getThresholdNum())
                : (rule.getThresholdLen() != null) ? String.valueOf(rule.getThresholdLen()) : "";
        String status = run.getSuccess() != null ? (run.getSuccess() ? "SUCCESS" : "FAIL") : "UNKNOWN";

        // 시간 포맷팅
        // ${startedAt}, ${finishedAt}는 한국 시간(KST)으로 표시
        ZoneId koreaZone = ZoneId.of("Asia/Seoul");
        String startedAtStr = formatDateTime(run.getStartedAt(), koreaZone);
        String finishedAtStr = formatDateTime(run.getFinishedAt(), koreaZone);
        // ${startedAtLocal}, ${finishedAtLocal}는 서버 현지 시간으로 표시
        String startedAtLocalStr = formatDateTimeLocal(run.getStartedAt(), serverZoneId);
        String finishedAtLocalStr = formatDateTimeLocal(run.getFinishedAt(), serverZoneId);
        // 날짜/시간도 한국 시간으로 표시
        String startedDateStr = formatDate(run.getStartedAt(), koreaZone);
        String finishedDateStr = formatDate(run.getFinishedAt(), koreaZone);
        String startedTimeStr = formatTime(run.getStartedAt(), koreaZone);
        String finishedTimeStr = formatTime(run.getFinishedAt(), koreaZone);
        String durationStr = formatDuration(run.getDurationMs());

        // 변수 치환 (${var} 형식)
        template = template.replace("${ruleName}", safe(rule.getName()));
        template = template.replace("${ruleId}", safeNum(rule.getId()));
        template = template.replace("${serverId}", safeNum(rule.getServerId()));
        template = template.replace("${serverName}", safe(serverName));
        template = template.replace("${serverHost}", safe(serverHost));
        template = template.replace("${serverTimezone}", safe(serverTimezone));
        template = template.replace("${outputNum}", outputNum != null ? String.valueOf(outputNum) : "");
        template = template.replace("${outputLen}", String.valueOf(outputLen));
        template = template.replace("${threshold}", threshold);
        template = template.replace("${thresholdNum}", rule.getThresholdNum() != null ? String.valueOf(rule.getThresholdNum()) : "");
        template = template.replace("${thresholdLen}", rule.getThresholdLen() != null ? String.valueOf(rule.getThresholdLen()) : "");
        template = template.replace("${pattern}", safe(rule.getPattern()));
        template = template.replace("${status}", status);
        template = template.replace("${success}", String.valueOf(run.getSuccess()));
        template = template.replace("${output}", safe(run.getOutput()));
        template = template.replace("${error}", safe(run.getErrorMessage()));
        template = template.replace("${startedAt}", startedAtStr);
        template = template.replace("${finishedAt}", finishedAtStr);
        template = template.replace("${startedAtLocal}", startedAtLocalStr);
        template = template.replace("${finishedAtLocal}", finishedAtLocalStr);
        template = template.replace("${startedDate}", startedDateStr);
        template = template.replace("${finishedDate}", finishedDateStr);
        template = template.replace("${startedTime}", startedTimeStr);
        template = template.replace("${finishedTime}", finishedTimeStr);
        template = template.replace("${durationMs}", safeNum(run.getDurationMs()));
        template = template.replace("${duration}", durationStr);
        template = template.replace("${monitoringType}", rule.getMonitoringType() != null ? rule.getMonitoringType().name() : "");
        template = template.replace("${alertOperator}", rule.getAlertOperator() != null ? rule.getAlertOperator().name() : "");

        // {var} 형식도 치환 (레거시 호환)
        template = template.replace("{ruleName}", safe(rule.getName()));
        template = template.replace("{ruleId}", safeNum(rule.getId()));
        template = template.replace("{serverId}", safeNum(rule.getServerId()));
        template = template.replace("{serverName}", safe(serverName));
        template = template.replace("{serverHost}", safe(serverHost));
        template = template.replace("{serverTimezone}", safe(serverTimezone));
        template = template.replace("{outputNum}", outputNum != null ? String.valueOf(outputNum) : "");
        template = template.replace("{outputLen}", String.valueOf(outputLen));
        template = template.replace("{threshold}", threshold);
        template = template.replace("{thresholdNum}", rule.getThresholdNum() != null ? String.valueOf(rule.getThresholdNum()) : "");
        template = template.replace("{thresholdLen}", rule.getThresholdLen() != null ? String.valueOf(rule.getThresholdLen()) : "");
        template = template.replace("{pattern}", safe(rule.getPattern()));
        template = template.replace("{status}", status);
        template = template.replace("{success}", String.valueOf(run.getSuccess()));
        template = template.replace("{output}", safe(run.getOutput()));
        template = template.replace("{error}", safe(run.getErrorMessage()));
        template = template.replace("{startedAt}", startedAtStr);
        template = template.replace("{finishedAt}", finishedAtStr);
        template = template.replace("{startedAtLocal}", startedAtLocalStr);
        template = template.replace("{finishedAtLocal}", finishedAtLocalStr);
        template = template.replace("{startedDate}", startedDateStr);
        template = template.replace("{finishedDate}", finishedDateStr);
        template = template.replace("{startedTime}", startedTimeStr);
        template = template.replace("{finishedTime}", finishedTimeStr);
        template = template.replace("{durationMs}", safeNum(run.getDurationMs()));
        template = template.replace("{duration}", durationStr);
        template = template.replace("{monitoringType}", rule.getMonitoringType() != null ? rule.getMonitoringType().name() : "");
        template = template.replace("{alertOperator}", rule.getAlertOperator() != null ? rule.getAlertOperator().name() : "");

        return template;
    }

    private String safe(String s) {
        return (s == null) ? "" : s;
    }

    private String safeNum(Object n) {
        return (n == null) ? "" : String.valueOf(n);
    }

    private String formatDateTime(OffsetDateTime dt, ZoneId serverZoneId) {
        if (dt == null) return "";
        try {
            if (serverZoneId != null) {
                return dt.atZoneSameInstant(serverZoneId).format(DEFAULT_DATETIME_FORMATTER);
            }
            return dt.format(DEFAULT_DATETIME_FORMATTER);
        } catch (Exception e) {
            return dt.toString();
        }
    }

    private String formatDateTimeLocal(OffsetDateTime dt, ZoneId serverZoneId) {
        if (dt == null) return "";
        try {
            if (serverZoneId != null) {
                return dt.atZoneSameInstant(serverZoneId).format(DEFAULT_DATETIME_FORMATTER) + " (현지시각)";
            }
            return dt.format(DEFAULT_DATETIME_FORMATTER);
        } catch (Exception e) {
            return dt.toString();
        }
    }

    private String formatDate(OffsetDateTime dt, ZoneId serverZoneId) {
        if (dt == null) return "";
        try {
            if (serverZoneId != null) {
                return dt.atZoneSameInstant(serverZoneId).format(DEFAULT_DATE_FORMATTER);
            }
            return dt.format(DEFAULT_DATE_FORMATTER);
        } catch (Exception e) {
            return dt.toLocalDate().toString();
        }
    }

    private String formatTime(OffsetDateTime dt, ZoneId serverZoneId) {
        if (dt == null) return "";
        try {
            if (serverZoneId != null) {
                return dt.atZoneSameInstant(serverZoneId).format(DEFAULT_TIME_FORMATTER);
            }
            return dt.format(DEFAULT_TIME_FORMATTER);
        } catch (Exception e) {
            return dt.toLocalTime().toString();
        }
    }

    private String formatDuration(Long durationMs) {
        if (durationMs == null) return "";
        long seconds = durationMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        if (hours > 0) {
            return String.format("%d시간 %d분 %d초", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%d분 %d초", minutes, seconds % 60);
        } else {
            return String.format("%d초", seconds);
        }
    }

    private Double extractOutputNum(String output) {
        if (output == null || output.isBlank()) {
            return null;
        }
        try {
            // 디스크 공간 모니터링의 경우: "53%" 형식의 사용률을 추출
            // 정규식으로 "숫자%" 패턴을 찾아 가장 높은 값을 반환
            java.util.regex.Pattern percentPattern = java.util.regex.Pattern.compile("(\\d+(?:\\.\\d+)?)%");
            java.util.regex.Matcher matcher = percentPattern.matcher(output);
            Double maxPercent = null;
            while (matcher.find()) {
                try {
                    Double percent = Double.parseDouble(matcher.group(1));
                    if (maxPercent == null || percent > maxPercent) {
                        maxPercent = percent;
                    }
                } catch (NumberFormatException e) {
                    // 무시하고 계속
                }
            }
            
            // 사용률(%)이 발견되면 반환
            if (maxPercent != null) {
                return maxPercent;
            }
            
            // 사용률이 없으면 일반 숫자 추출 시도
            String cleaned = output.trim().replaceAll("[^0-9.-]", "");
            if (cleaned.isEmpty()) {
                return null;
            }
            return Double.parseDouble(cleaned);
        } catch (Exception e) {
            return null;
        }
    }

    private NotificationChannel parseChannel(String ch) {
        try {
            return NotificationChannel.valueOf(ch);
        } catch (Exception e) {
            return null;
        }
    }

    private String getRecipientAddress(AlertRecipientEntity recipient, NotificationChannel channel) {
        return switch (channel) {
            case SMS -> recipient.getPhone();
            case EMAIL -> recipient.getEmail();
            case KAKAO -> recipient.getKakao();
        };
    }
}
