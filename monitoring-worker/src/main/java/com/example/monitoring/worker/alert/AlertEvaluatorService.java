package com.example.monitoring.worker.alert;

import com.example.monitoring.common.domain.*;
import com.example.monitoring.common.repo.AlertRuleRecipientLinkRepository;
import com.example.monitoring.common.repo.AlertRuleRepository;
import com.example.monitoring.common.repo.CheckRepository;
import com.example.monitoring.common.repo.NotificationOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AlertEvaluatorService {

    private static final Logger log = LoggerFactory.getLogger(AlertEvaluatorService.class);

    private final AlertRuleRepository ruleRepo;
    private final AlertRuleRecipientLinkRepository linkRepo;
    private final NotificationOutboxRepository outboxRepo;
    private final CheckRepository checkRepository;

    public AlertEvaluatorService(
            AlertRuleRepository ruleRepo,
            AlertRuleRecipientLinkRepository linkRepo,
            NotificationOutboxRepository outboxRepo,
            CheckRepository checkRepository
    ) {
        this.ruleRepo = ruleRepo;
        this.linkRepo = linkRepo;
        this.outboxRepo = outboxRepo;
        this.checkRepository = checkRepository;
    }

    @Transactional
    public void evaluateAndEnqueue(CheckRunEntity run) {
        List<AlertRuleEntity> rules = ruleRepo.findByEnabledTrue();
        if (rules.isEmpty()) return;

        OffsetDateTime now = OffsetDateTime.now();

        for (AlertRuleEntity rule : rules) {
            if (!isScopeMatch(rule, run)) continue;
            if (!isCooldownOk(rule, now)) continue;
            if (!evaluateRule(rule, run)) continue;

            // fired
            rule.setLastFiredAt(now);
            ruleRepo.save(rule);

            enqueue(rule, run, now);
        }
    }

    private boolean isScopeMatch(AlertRuleEntity rule, CheckRunEntity run) {
        if (rule.getCheckId() != null && !Objects.equals(rule.getCheckId(), run.getCheckId())) return false;
        return true;
    }

    private boolean isCooldownOk(AlertRuleEntity rule, OffsetDateTime now) {
        Integer cd = rule.getCooldownSec();
        if (cd == null || cd <= 0) return true;
        OffsetDateTime last = rule.getLastFiredAt();
        if (last == null) return true;
        return last.plusSeconds(cd).isBefore(now);
    }

    private boolean evaluateRule(AlertRuleEntity rule, CheckRunEntity run) {
        String output = (run.getOutput() == null) ? "" : run.getOutput();

        return switch (rule.getRuleType()) {
            case RUN_FAILED -> Boolean.FALSE.equals(run.getSuccess());
            case OUTPUT_LEN_GT -> output.length() > ((rule.getThresholdLen() == null) ? 0 : rule.getThresholdLen());
            case OUTPUT_LEN_LT -> {
                Integer th = rule.getThresholdLen();
                yield th != null && output.length() < th;
            }
            case OUTPUT_CONTAINS -> {
                String p = rule.getPattern();
                yield StringUtils.hasText(p) && output.contains(p);
            }
            case OUTPUT_NUM_GT -> {
                Double th = rule.getThresholdNum();
                Double val = tryParseDouble(output.trim());
                yield (th != null && val != null && val > th);
            }
            case OUTPUT_NUM_LT -> {
                Double th = rule.getThresholdNum();
                Double val = tryParseDouble(output.trim());
                yield (th != null && val != null && val < th);
            }
            case OUTPUT_REGEX_NUM_GT -> {
                Double th = rule.getThresholdNum();
                String regex = rule.getPattern();
                if (th == null || !StringUtils.hasText(regex)) yield false;
                Double extracted = extractFirstNumberByRegex(output, regex);
                yield extracted != null && extracted > th;
            }
        };
    }

    private Double tryParseDouble(String s) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            return null;
        }
    }

    private Double extractFirstNumberByRegex(String output, String regex) {
        try {
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(output);
            if (!m.find()) return null;
            String g = (m.groupCount() >= 1) ? m.group(1) : m.group();
            if (!StringUtils.hasText(g)) return null;
            return Double.parseDouble(g.trim());
        } catch (Exception e) {
            log.warn("Regex extract failed. regex={}", regex, e);
            return null;
        }
    }

    private void enqueue(AlertRuleEntity rule, CheckRunEntity run, OffsetDateTime now) {
        // link와 함께 alert_recipients(수신자) JOIN FETCH 로 로드
        List<AlertRuleRecipientLinkEntity> links = linkRepo.findByRuleIdAndEnabledTrue(rule.getId());
        if (links.isEmpty()) return;

        Set<NotificationChannel> ruleChannels = parseChannels(rule.getChannels()); // optional
        String body = renderMessage(rule, run);

        for (AlertRuleRecipientLinkEntity link : links) {
            AlertRecipientEntity r = link.getRecipient();  // alert_recipients
            if (r == null || !Boolean.TRUE.equals(r.getEnabled())) continue;

            Set<NotificationChannel> rc = parseChannels(r.getChannels());
            Set<NotificationChannel> finalChannels = ruleChannels.isEmpty() ? rc : intersect(ruleChannels, rc);

            for (NotificationChannel ch : finalChannels) {
                String to = resolveTo(r, ch);
                if (!StringUtils.hasText(to)) continue;

                NotificationOutboxEntity n = new NotificationOutboxEntity();
                n.setStatus(NotificationStatus.PENDING);
                n.setChannel(ch);
                n.setRuleId(rule.getId());
                n.setCheckRunId(run.getId());
                n.setToAddr(to);
                n.setTitle("[MON] " + safe(rule.getName()));
                n.setBody(body);

                n.setAttempt(0);
                n.setMaxAttempt(5);
                n.setNextAttemptAt(now);

                outboxRepo.save(n);
            }
        }
    }

    private String resolveTo(AlertRecipientEntity r, NotificationChannel ch) {
        return switch (ch) {
            case SMS -> r.getPhone();
            case EMAIL -> r.getEmail();
            case KAKAO -> resolveKakaoTo(r);
        };
    }

    /** KAKAO: Aligo 알림톡은 전화번호 필수. 전화번호 우선, 없으면 카카오 ID */
    private String resolveKakaoTo(AlertRecipientEntity r) {
        if (StringUtils.hasText(r.getPhone()) && r.getPhone().replaceAll("[^0-9]", "").length() >= 10) {
            return r.getPhone();
        }
        return r.getKakao();
    }

    private Set<NotificationChannel> parseChannels(String csv) {
        if (!StringUtils.hasText(csv)) return Collections.emptySet();
        EnumSet<NotificationChannel> set = EnumSet.noneOf(NotificationChannel.class);
        for (String token : csv.split(",")) {
            String t = token.trim();
            if (!StringUtils.hasText(t)) continue;
            try {
                set.add(NotificationChannel.valueOf(t));
            } catch (Exception ignored) {
            }
        }
        return set;
    }

    private Set<NotificationChannel> intersect(Set<NotificationChannel> a, Set<NotificationChannel> b) {
        EnumSet<NotificationChannel> x = EnumSet.noneOf(NotificationChannel.class);
        for (NotificationChannel c : a) if (b.contains(c)) x.add(c);
        return x;
    }

    private String renderMessage(AlertRuleEntity rule, CheckRunEntity run) {
        String tpl = rule.getMessageTemplate();
        if (!StringUtils.hasText(tpl)) {
            tpl = """
                    [MONITORING ALERT]
                    rule=${ruleName}
                    checkId=${checkId}
                    success=${success}
                    output=${output}
                    error=${error}
                    startedAt=${startedAt}
                    finishedAt=${finishedAt}
                    durationMs=${durationMs}
                    """;
        }

        var checkOpt = run.getCheckId() != null ? checkRepository.findById(run.getCheckId()) : Optional.<CheckEntity>empty();
        String targetName = checkOpt.map(c -> c.getTargetName() != null ? c.getTargetName() : "").orElse("");
        String checkName = checkOpt.map(c -> c.getName() != null ? c.getName() : "").orElse("");
        int outputLen = (run.getOutput() != null) ? run.getOutput().length() : 0;
        String threshold = (rule.getThresholdNum() != null) ? String.valueOf(rule.getThresholdNum())
                : (rule.getThresholdLen() != null) ? String.valueOf(rule.getThresholdLen()) : "";
        String status = run.getSuccess() != null ? (run.getSuccess() ? "SUCCESS" : "FAIL") : "UNKNOWN";

        return replaceVars(tpl, rule, run, targetName, checkName, outputLen, threshold, status);
    }

    private String replaceVars(String tpl, AlertRuleEntity rule, CheckRunEntity run,
                               String targetName, String checkName, int outputLen, String threshold, String status) {
        String result = tpl
                .replace("${ruleName}", safe(rule.getName()))
                .replace("${checkId}", safeNum(run.getCheckId()))
                .replace("${serverId}", safeNum(run.getCheckId()))
                .replace("${targetName}", safe(targetName))
                .replace("${checkName}", safe(checkName))
                .replace("${serverName}", safe(targetName))
                .replace("${outputLen}", String.valueOf(outputLen))
                .replace("${threshold}", threshold)
                .replace("${status}", status)
                .replace("${success}", String.valueOf(run.getSuccess()))
                .replace("${output}", safe(run.getOutput()))
                .replace("${error}", safe(run.getErrorMessage()))
                .replace("${startedAt}", safeTime(run.getStartedAt()))
                .replace("${finishedAt}", safeTime(run.getFinishedAt()))
                .replace("${durationMs}", safeNum(run.getDurationMs()));
        // {var} 형식도 치환 (seed/레거시 호환)
        return result
                .replace("{ruleName}", safe(rule.getName()))
                .replace("{checkId}", safeNum(run.getCheckId()))
                .replace("{serverId}", safeNum(run.getCheckId()))
                .replace("{targetName}", safe(targetName))
                .replace("{checkName}", safe(checkName))
                .replace("{serverName}", safe(targetName))
                .replace("{outputLen}", String.valueOf(outputLen))
                .replace("{threshold}", threshold)
                .replace("{status}", status)
                .replace("{success}", String.valueOf(run.getSuccess()))
                .replace("{output}", safe(run.getOutput()))
                .replace("{error}", safe(run.getErrorMessage()))
                .replace("{startedAt}", safeTime(run.getStartedAt()))
                .replace("{finishedAt}", safeTime(run.getFinishedAt()))
                .replace("{durationMs}", safeNum(run.getDurationMs()));
    }

    private String safe(String s) {
        return (s == null) ? "" : s;
    }

    private String safeNum(Object n) {
        return (n == null) ? "" : String.valueOf(n);
    }

    private String safeTime(OffsetDateTime t) {
        return (t == null) ? "" : t.toString();
    }
}