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
            // Rule 2번 디버깅을 위한 상세 로깅
            if (rule.getId() != null && rule.getId() == 2L) {
                log.info("[Rule 2] 평가 시작. ruleId={}, checkId={}, run.checkId={}", 
                        rule.getId(), rule.getCheckId(), run.getCheckId());
            }
            
            if (!isScopeMatch(rule, run)) {
                if (rule.getId() != null && rule.getId() == 2L) {
                    log.warn("[Rule 2] Scope 불일치. rule.checkId={}, run.checkId={}", 
                            rule.getCheckId(), run.getCheckId());
                }
                continue;
            }
            if (!isCooldownOk(rule, now)) {
                if (rule.getId() != null && rule.getId() == 2L) {
                    log.warn("[Rule 2] Cooldown 시간 미경과. cooldownSec={}, lastFiredAt={}", 
                            rule.getCooldownSec(), rule.getLastFiredAt());
                }
                continue;
            }
            if (!evaluateRule(rule, run)) {
                if (rule.getId() != null && rule.getId() == 2L) {
                    log.warn("[Rule 2] 규칙 조건 불만족. ruleType={}, thresholdNum={}, thresholdLen={}, pattern={}, run.success={}, output={}", 
                            rule.getRuleType(), rule.getThresholdNum(), rule.getThresholdLen(), 
                            rule.getPattern(), run.getSuccess(), 
                            run.getOutput() != null ? run.getOutput().substring(0, Math.min(100, run.getOutput().length())) : "null");
                }
                continue;
            }

            // fired
            if (rule.getId() != null && rule.getId() == 2L) {
                log.info("[Rule 2] 규칙 조건 만족. 알림 큐에 추가 시작.");
            }
            
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
        
        // Rule 2번 디버깅을 위한 상세 로깅
        if (rule.getId() != null && rule.getId() == 2L) {
            log.info("[Rule 2] enqueue 시작. links.size={}", links.size());
        }
        
        if (links.isEmpty()) {
            if (rule.getId() != null && rule.getId() == 2L) {
                log.warn("[Rule 2] 수신자 링크가 없습니다. ruleId={}", rule.getId());
            }
            return;
        }

        Set<NotificationChannel> ruleChannels = parseChannels(rule.getChannels()); // Rule에 설정된 채널
        String body = renderMessage(rule, run);
        
        if (rule.getId() != null && rule.getId() == 2L) {
            log.info("[Rule 2] ruleChannels={}, body.length={}", ruleChannels, body != null ? body.length() : 0);
        }

        int enqueuedCount = 0;
        for (AlertRuleRecipientLinkEntity link : links) {
            AlertRecipientEntity r = link.getRecipient();  // alert_recipients
            if (r == null || !Boolean.TRUE.equals(r.getEnabled())) {
                if (rule.getId() != null && rule.getId() == 2L) {
                    log.warn("[Rule 2] 수신자가 비활성화됨. recipientId={}, enabled={}", 
                            r != null ? r.getId() : null, r != null ? r.getEnabled() : null);
                }
                continue;
            }

            // Rule에 채널이 설정되어 있으면 그것만 사용, 없으면 수신자의 채널 사용
            Set<NotificationChannel> finalChannels;
            if (!ruleChannels.isEmpty()) {
                // Rule에 설정된 채널만 사용
                finalChannels = ruleChannels;
            } else {
                // Rule에 채널이 없으면 수신자의 채널 사용
                Set<NotificationChannel> rc = parseChannels(r.getChannels());
                finalChannels = rc;
            }
            
            if (rule.getId() != null && rule.getId() == 2L) {
                log.info("[Rule 2] 수신자 처리. recipientId={}, finalChannels={}", r.getId(), finalChannels);
            }

            for (NotificationChannel ch : finalChannels) {
                String to = resolveTo(r, ch);
                if (!StringUtils.hasText(to)) {
                    if (rule.getId() != null && rule.getId() == 2L) {
                        log.warn("[Rule 2] 수신자 주소 없음. channel={}, recipientId={}", ch, r.getId());
                    }
                    continue;
                }

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
                enqueuedCount++;
                
                if (rule.getId() != null && rule.getId() == 2L) {
                    log.info("[Rule 2] 알림 큐에 추가됨. channel={}, to={}, outboxId={}", ch, to, n.getId());
                }
            }
        }
        
        if (rule.getId() != null && rule.getId() == 2L) {
            log.info("[Rule 2] enqueue 완료. 총 {}개 알림 큐에 추가됨", enqueuedCount);
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
        
        // outputNum: output에서 숫자 추출 (규칙 타입에 따라 다르게 추출)
        String outputNum = extractOutputNum(rule, run);

        return replaceVars(tpl, rule, run, targetName, checkName, outputLen, threshold, status, outputNum);
    }
    
    /**
     * output에서 숫자 값을 추출
     * 규칙 타입에 따라 추출 방법이 다름
     */
    private String extractOutputNum(AlertRuleEntity rule, CheckRunEntity run) {
        String output = (run.getOutput() == null) ? "" : run.getOutput();
        if (!StringUtils.hasText(output)) return "";
        
        return switch (rule.getRuleType()) {
            case OUTPUT_NUM_GT, OUTPUT_NUM_LT -> {
                // output 전체를 숫자로 파싱 시도
                Double val = tryParseDouble(output.trim());
                yield (val != null) ? String.valueOf(val) : "";
            }
            case OUTPUT_REGEX_NUM_GT -> {
                // regex 패턴으로 숫자 추출
                String regex = rule.getPattern();
                if (StringUtils.hasText(regex)) {
                    Double extracted = extractFirstNumberByRegex(output, regex);
                    yield (extracted != null) ? String.valueOf(extracted) : "";
                }
                yield "";
            }
            default -> {
                // 기본: output에서 첫 번째 숫자 추출 시도
                Double val = tryParseDouble(output.trim());
                if (val != null) {
                    yield String.valueOf(val);
                }
                // 숫자 파싱 실패 시 정규식으로 첫 숫자 찾기
                Double extracted = extractFirstNumberByRegex(output, "(-?\\d+(?:\\.\\d+)?)");
                yield (extracted != null) ? String.valueOf(extracted) : "";
            }
        };
    }

    private String replaceVars(String tpl, AlertRuleEntity rule, CheckRunEntity run,
                               String targetName, String checkName, int outputLen, String threshold, String status, String outputNum) {
        String result = tpl
                .replace("${ruleName}", safe(rule.getName()))
                .replace("${checkId}", safeNum(run.getCheckId()))
                .replace("${serverId}", safeNum(run.getCheckId()))
                .replace("${targetName}", safe(targetName))
                .replace("${checkName}", safe(checkName))
                .replace("${serverName}", safe(targetName))
                .replace("${outputLen}", String.valueOf(outputLen))
                .replace("${outputNum}", safe(outputNum))
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
                .replace("{outputNum}", safe(outputNum))
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