package com.example.monitoring.web.service;

import com.example.monitoring.common.domain.MonitoringRuleEntity;
import com.example.monitoring.common.domain.ServerEntity;
import com.example.monitoring.common.repo.MonitoringRuleRepository;
import com.example.monitoring.common.repo.ServerRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 메시지 템플릿 테스트 서비스
 * 실제 알림을 발송하여 템플릿을 테스트합니다.
 */
@Service
public class MessageTemplateTestService {

    private static final Logger log = LoggerFactory.getLogger(MessageTemplateTestService.class);
    private static final DateTimeFormatter DEFAULT_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final String ALIMTALK_URL = "https://kakaoapi.aligo.in/akv10/alimtalk/send/";
    private static final String SMS_URL = "https://apis.aligo.in/send/";

    @Value("${aligo.api-key:}")
    private String apiKey;

    @Value("${aligo.user-id:}")
    private String userId;

    @Value("${aligo.sender:}")
    private String sender;

    @Value("${aligo.sender-key:}")
    private String senderKey;

    @Value("${aligo.template-code:}")
    private String templateCode;

    @Value("${aligo.test-mode:N}")
    private String testMode;

    private final MonitoringRuleRepository ruleRepository;
    private final ServerRepository serverRepository;
    private final com.example.monitoring.common.repo.KakaoTemplateRepository kakaoTemplateRepository;
    private final com.example.monitoring.common.repo.VpnConnectionRepository vpnRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public MessageTemplateTestService(MonitoringRuleRepository ruleRepository,
                                     ServerRepository serverRepository,
                                     com.example.monitoring.common.repo.KakaoTemplateRepository kakaoTemplateRepository,
                                     com.example.monitoring.common.repo.VpnConnectionRepository vpnRepository) {
        this.ruleRepository = ruleRepository;
        this.serverRepository = serverRepository;
        this.kakaoTemplateRepository = kakaoTemplateRepository;
        this.vpnRepository = vpnRepository;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 메시지 템플릿 테스트 결과
     */
    public static class TestResult {
        private final boolean success;
        private final String message;

        public TestResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public static TestResult success(String message) {
            return new TestResult(true, message);
        }

        public static TestResult fail(String message) {
            return new TestResult(false, message);
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * 카카오 템플릿을 사용하여 테스트 알림을 발송
     */
    public TestResult testKakaoTemplate(Long ruleId, String testRecipient) {
        try {
            // 규칙 정보 조회
            MonitoringRuleEntity rule = ruleRepository.findById(ruleId)
                    .orElseThrow(() -> new IllegalArgumentException("규칙을 찾을 수 없습니다: " + ruleId));

            // 카카오 템플릿 코드 확인
            String templateCode = rule.getKakaoTemplateCode();
            if (!StringUtils.hasText(templateCode)) {
                return TestResult.fail("카카오 템플릿이 선택되지 않았습니다. 알림 규칙에서 카카오 템플릿을 선택하세요.");
            }

            // 템플릿 엔티티 조회
            com.example.monitoring.common.domain.KakaoTemplateEntity template = kakaoTemplateRepository
                    .findByTemplateCode(templateCode)
                    .orElse(null);
            
            if (template == null) {
                return TestResult.fail("카카오 템플릿을 찾을 수 없습니다. 템플릿 코드: " + templateCode);
            }

            // 템플릿 변수 값 렌더링
            String templateVariablesJson = rule.getKakaoTemplateVariables();
            Map<String, String> templateVars = null;
            String finalMessage = null;
            String buttonInfo = template.getButtonInfo();

            if (StringUtils.hasText(templateVariablesJson)) {
                try {
                    // JSON 파싱
                    templateVars = objectMapper.readValue(templateVariablesJson,
                            objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, String.class));

                    // 템플릿 메시지 형태가 있으면 변수 치환하여 메시지 조합
                    String templateMessage = template.getTemplateMessage();
                    if (StringUtils.hasText(templateMessage) && templateVars != null) {
                        // 템플릿 메시지 형태에서 #{변수명} 형식을 변수 값으로 치환
                        finalMessage = templateMessage;
                        for (Map.Entry<String, String> entry : templateVars.entrySet()) {
                            String varName = entry.getKey();
                            String varValue = renderTemplateVariable(entry.getValue(), rule);
                            // #{변수명} 형식을 변수 값으로 치환
                            finalMessage = finalMessage.replace("#{" + varName + "}", varValue);
                        }
                        log.info("카카오 템플릿 메시지 형태 사용하여 테스트 메시지 생성: ruleId={}, templateCode={}", ruleId, templateCode);
                    } else {
                        // 템플릿 변수 값을 JSON 문자열로 변환
                        finalMessage = objectMapper.writeValueAsString(templateVars);
                        log.info("카카오 템플릿 변수 사용하여 테스트 메시지 생성: ruleId={}, templateCode={}", ruleId, templateCode);
                    }
                } catch (Exception e) {
                    log.warn("카카오 템플릿 변수 메시지 생성 실패: ruleId={}, error={}", ruleId, e.getMessage(), e);
                    return TestResult.fail("템플릿 변수 처리 실패: " + e.getMessage());
                }
            } else {
                // 템플릿 변수가 없으면 기본 메시지 사용
                finalMessage = template.getTemplateMessage();
                if (!StringUtils.hasText(finalMessage)) {
                    finalMessage = rule.getName() + " 모니터링에 알림이 발생했습니다.";
                }
            }

            // 카카오 알림 발송
            return sendKakaoTestWithTemplate(testRecipient, rule.getName(), finalMessage, templateCode, buttonInfo);
        } catch (Exception e) {
            log.error("카카오 템플릿 테스트 실패: ruleId={}", ruleId, e);
            return TestResult.fail("테스트 실패: " + e.getMessage());
        }
    }

    /**
     * 템플릿 변수 값 내부의 ${변수명}을 실제 값으로 치환
     */
    private String renderTemplateVariable(String varTemplate, MonitoringRuleEntity rule) {
        if (!StringUtils.hasText(varTemplate)) {
            return "";
        }

        // 서버 정보 조회
        ServerEntity server = null;
        String serverName = "";
        String serverHost = "";
        String serverTimezone = null;
        if (rule.getServerId() != null) {
            Optional<ServerEntity> serverOpt = serverRepository.findById(rule.getServerId());
            if (serverOpt.isPresent()) {
                server = serverOpt.get();
                serverName = server.getName() != null ? server.getName() : "";
                serverHost = server.getHost() != null ? server.getHost() : "";
                serverTimezone = server.getTimezone();
            }
        }

        // 더미 체크 실행 데이터 생성
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Asia/Seoul"));
        Double outputNum = 100.0;
        int outputLen = 50;
        String threshold = rule.getThresholdNum() != null ? String.valueOf(rule.getThresholdNum())
                : (rule.getThresholdLen() != null ? String.valueOf(rule.getThresholdLen()) : "");
        String status = "FAIL";

        // 시간 포맷팅
        ZoneId koreaZone = ZoneId.of("Asia/Seoul");
        String startedAtStr = formatDateTime(now.minusSeconds(10), koreaZone);
        String finishedAtStr = formatDateTime(now, koreaZone);
        String startedAtLocalStr = startedAtStr;
        String finishedAtLocalStr = finishedAtStr;
        String startedDateStr = formatDate(now.minusSeconds(10), koreaZone);
        String finishedDateStr = formatDate(now, koreaZone);
        String startedTimeStr = formatTime(now.minusSeconds(10), koreaZone);
        String finishedTimeStr = formatTime(now, koreaZone);
        String durationStr = "10초";

        // 변수 치환
        String rendered = varTemplate;
        rendered = rendered.replace("${ruleName}", safe(rule.getName()));
        rendered = rendered.replace("${ruleId}", safeNum(rule.getId()));
        rendered = rendered.replace("${serverId}", safeNum(rule.getServerId()));
        rendered = rendered.replace("${serverName}", safe(serverName));
        rendered = rendered.replace("${serverHost}", safe(serverHost));
        rendered = rendered.replace("${serverTimezone}", safe(serverTimezone));
        rendered = rendered.replace("${outputNum}", outputNum != null ? String.valueOf(outputNum) : "");
        rendered = rendered.replace("${outputLen}", String.valueOf(outputLen));
        rendered = rendered.replace("${threshold}", threshold);
        rendered = rendered.replace("${thresholdNum}", rule.getThresholdNum() != null ? String.valueOf(rule.getThresholdNum()) : "");
        rendered = rendered.replace("${thresholdLen}", rule.getThresholdLen() != null ? String.valueOf(rule.getThresholdLen()) : "");
        rendered = rendered.replace("${pattern}", safe(rule.getPattern()));
        rendered = rendered.replace("${status}", status);
        rendered = rendered.replace("${success}", "false");
        rendered = rendered.replace("${output}", "테스트 출력값");
        rendered = rendered.replace("${error}", "");
        rendered = rendered.replace("${startedAt}", startedAtStr);
        rendered = rendered.replace("${finishedAt}", finishedAtStr);
        rendered = rendered.replace("${startedAtLocal}", startedAtLocalStr);
        rendered = rendered.replace("${finishedAtLocal}", finishedAtLocalStr);
        rendered = rendered.replace("${startedDate}", startedDateStr);
        rendered = rendered.replace("${finishedDate}", finishedDateStr);
        rendered = rendered.replace("${startedTime}", startedTimeStr);
        rendered = rendered.replace("${finishedTime}", finishedTimeStr);
        rendered = rendered.replace("${durationMs}", "10000");
        rendered = rendered.replace("${duration}", durationStr);
        rendered = rendered.replace("${monitoringType}", rule.getMonitoringType() != null ? rule.getMonitoringType().name() : "");
        rendered = rendered.replace("${alertOperator}", rule.getAlertOperator() != null ? rule.getAlertOperator().name() : "");

        return rendered;
    }

    /**
     * VPN 카카오 템플릿을 사용하여 테스트 알림을 발송
     */
    public TestResult testVpnKakaoTemplate(Long vpnId, String testRecipient) {
        try {
            // VPN 정보 조회
            com.example.monitoring.common.domain.VpnConnectionEntity vpn = vpnRepository.findById(vpnId)
                    .orElseThrow(() -> new IllegalArgumentException("VPN을 찾을 수 없습니다: " + vpnId));

            // 카카오 템플릿 코드 확인
            String templateCode = vpn.getKakaoTemplateCode();
            if (!StringUtils.hasText(templateCode)) {
                return TestResult.fail("카카오 템플릿이 선택되지 않았습니다. VPN 설정에서 카카오 템플릿을 선택하세요.");
            }

            // 템플릿 엔티티 조회
            com.example.monitoring.common.domain.KakaoTemplateEntity template = kakaoTemplateRepository
                    .findByTemplateCode(templateCode)
                    .orElse(null);
            
            if (template == null) {
                return TestResult.fail("카카오 템플릿을 찾을 수 없습니다. 템플릿 코드: " + templateCode);
            }

            // 템플릿 변수 값 렌더링
            String templateVariablesJson = vpn.getKakaoTemplateVariables();
            Map<String, String> templateVars = null;
            String finalMessage = null;
            String buttonInfo = template.getButtonInfo();

            if (StringUtils.hasText(templateVariablesJson)) {
                try {
                    // JSON 파싱
                    templateVars = objectMapper.readValue(templateVariablesJson,
                            objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, String.class));

                    // 템플릿 메시지 형태가 있으면 변수 치환하여 메시지 조합
                    String templateMessage = template.getTemplateMessage();
                    if (StringUtils.hasText(templateMessage) && templateVars != null) {
                        // 템플릿 메시지 형태에서 #{변수명} 형식을 변수 값으로 치환
                        finalMessage = templateMessage;
                        for (Map.Entry<String, String> entry : templateVars.entrySet()) {
                            String varName = entry.getKey();
                            String varValue = renderVpnTemplateVariable(entry.getValue(), vpn);
                            // #{변수명} 형식을 변수 값으로 치환
                            finalMessage = finalMessage.replace("#{" + varName + "}", varValue);
                        }
                        log.info("VPN 카카오 템플릿 메시지 형태 사용하여 테스트 메시지 생성: vpnId={}, templateCode={}", vpnId, templateCode);
                    } else {
                        // 템플릿 변수 값을 JSON 문자열로 변환
                        finalMessage = objectMapper.writeValueAsString(templateVars);
                        log.info("VPN 카카오 템플릿 변수 사용하여 테스트 메시지 생성: vpnId={}, templateCode={}", vpnId, templateCode);
                    }
                } catch (Exception e) {
                    log.warn("VPN 카카오 템플릿 변수 메시지 생성 실패: vpnId={}, error={}", vpnId, e.getMessage(), e);
                    return TestResult.fail("템플릿 변수 처리 실패: " + e.getMessage());
                }
            } else {
                // 템플릿 변수가 없으면 기본 메시지 사용
                finalMessage = template.getTemplateMessage();
                if (!StringUtils.hasText(finalMessage)) {
                    finalMessage = vpn.getName() + " VPN 상태 변경 알림이 발생했습니다.";
                }
            }

            // 카카오 알림 발송
            return sendKakaoTestWithTemplate(testRecipient, vpn.getName(), finalMessage, templateCode, buttonInfo);
        } catch (Exception e) {
            log.error("VPN 카카오 템플릿 테스트 실패: vpnId={}", vpnId, e);
            return TestResult.fail("테스트 실패: " + e.getMessage());
        }
    }

    /**
     * VPN 템플릿 변수 값 내부의 ${변수명}을 실제 값으로 치환
     */
    private String renderVpnTemplateVariable(String varTemplate, com.example.monitoring.common.domain.VpnConnectionEntity vpn) {
        if (!StringUtils.hasText(varTemplate)) {
            return "";
        }

        // 더미 VPN 상태 변경 데이터 생성
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Asia/Seoul"));
        String changeTimeStr = formatDateTime(now, ZoneId.of("Asia/Seoul"));
        String changeDateStr = formatDate(now, ZoneId.of("Asia/Seoul"));
        String changeTimeOnlyStr = formatTime(now, ZoneId.of("Asia/Seoul"));
        String oldStatus = "DOWN";
        String newStatus = "UP";

        // 변수 치환
        String rendered = varTemplate;
        rendered = rendered.replace("${vpnName}", safe(vpn.getName()));
        rendered = rendered.replace("${vpnId}", safeNum(vpn.getId()));
        rendered = rendered.replace("${vpnHost}", safe(vpn.getHost()));
        rendered = rendered.replace("${oldStatus}", oldStatus);
        rendered = rendered.replace("${newStatus}", newStatus);
        rendered = rendered.replace("${changeTime}", changeTimeStr);
        rendered = rendered.replace("${changeDate}", changeDateStr);
        rendered = rendered.replace("${changeTimeOnly}", changeTimeOnlyStr);

        return rendered;
    }

    /**
     * 템플릿 렌더링 (테스트용 더미 데이터 사용)
     */
    private String renderTemplate(MonitoringRuleEntity rule, String template) {
        if (!StringUtils.hasText(template)) {
            template = "${ruleName}에 모니터링 알림이 발생하였습니다.";
        }

        // 서버 정보 조회
        ServerEntity server = null;
        String serverName = "";
        String serverHost = "";
        String serverTimezone = null;
        ZoneId serverZoneId = null;
        if (rule.getServerId() != null) {
            Optional<ServerEntity> serverOpt = serverRepository.findById(rule.getServerId());
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

        // 테스트용 더미 데이터
        OffsetDateTime now = OffsetDateTime.now();
        Double outputNum = 85.5; // 테스트용 더미 값
        int outputLen = 150; // 테스트용 더미 값
        String threshold = rule.getThresholdNum() != null ? String.valueOf(rule.getThresholdNum())
                : (rule.getThresholdLen() != null ? String.valueOf(rule.getThresholdLen()) : "80");
        String status = "FAIL";
        String output = "테스트 출력값입니다.\n이것은 메시지 템플릿 테스트를 위한 샘플 출력입니다.";
        String error = null;

        // 시간 포맷팅
        ZoneId koreaZone = ZoneId.of("Asia/Seoul");
        String startedAtStr = formatDateTime(now.minusSeconds(30), koreaZone);
        String finishedAtStr = formatDateTime(now, koreaZone);
        String startedAtLocalStr = formatDateTimeLocal(now.minusSeconds(30), serverZoneId);
        String finishedAtLocalStr = formatDateTimeLocal(now, serverZoneId);
        String startedDateStr = formatDate(now.minusSeconds(30), koreaZone);
        String finishedDateStr = formatDate(now, koreaZone);
        String startedTimeStr = formatTime(now.minusSeconds(30), koreaZone);
        String finishedTimeStr = formatTime(now, koreaZone);
        String durationStr = "30초";

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
        template = template.replace("${success}", "false");
        template = template.replace("${output}", safe(output));
        template = template.replace("${error}", safe(error));
        template = template.replace("${startedAt}", startedAtStr);
        template = template.replace("${finishedAt}", finishedAtStr);
        template = template.replace("${startedAtLocal}", startedAtLocalStr);
        template = template.replace("${finishedAtLocal}", finishedAtLocalStr);
        template = template.replace("${startedDate}", startedDateStr);
        template = template.replace("${finishedDate}", finishedDateStr);
        template = template.replace("${startedTime}", startedTimeStr);
        template = template.replace("${finishedTime}", finishedTimeStr);
        template = template.replace("${durationMs}", "30000");
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
        template = template.replace("{success}", "false");
        template = template.replace("{output}", safe(output));
        template = template.replace("{error}", safe(error));
        template = template.replace("{startedAt}", startedAtStr);
        template = template.replace("{finishedAt}", finishedAtStr);
        template = template.replace("{startedAtLocal}", startedAtLocalStr);
        template = template.replace("{finishedAtLocal}", finishedAtLocalStr);
        template = template.replace("{startedDate}", startedDateStr);
        template = template.replace("{finishedDate}", finishedDateStr);
        template = template.replace("{startedTime}", startedTimeStr);
        template = template.replace("{finishedTime}", finishedTimeStr);
        template = template.replace("{durationMs}", "30000");
        template = template.replace("{duration}", durationStr);
        template = template.replace("{monitoringType}", rule.getMonitoringType() != null ? rule.getMonitoringType().name() : "");
        template = template.replace("{alertOperator}", rule.getAlertOperator() != null ? rule.getAlertOperator().name() : "");

        return template;
    }

    /**
     * KAKAO 템플릿 변수 값을 사용하여 메시지 생성 (테스트용)
     */
    private String renderKakaoTemplateMessage(MonitoringRuleEntity rule, String template) throws Exception {
        String templateVariablesJson = rule.getKakaoTemplateVariables();
        if (!StringUtils.hasText(templateVariablesJson)) {
            return renderTemplate(rule, template); // 기본 메시지 사용
        }

        // JSON 파싱
        Map<String, String> templateVars = objectMapper.readValue(templateVariablesJson, 
                objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, String.class));

        // 서버 정보 조회
        ServerEntity server = null;
        String serverName = "";
        String serverHost = "";
        String serverTimezone = null;
        ZoneId serverZoneId = null;
        if (rule.getServerId() != null) {
            Optional<ServerEntity> serverOpt = serverRepository.findById(rule.getServerId());
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

        // 더미 체크 실행 데이터 생성
        OffsetDateTime now = OffsetDateTime.now(ZoneId.of("Asia/Seoul"));
        Double outputNum = 100.0;
        int outputLen = 50;
        String threshold = rule.getThresholdNum() != null ? String.valueOf(rule.getThresholdNum())
                : (rule.getThresholdLen() != null ? String.valueOf(rule.getThresholdLen()) : "");
        String status = "FAIL";

        // 시간 포맷팅
        ZoneId koreaZone = ZoneId.of("Asia/Seoul");
        String startedAtStr = now.minusSeconds(10).format(DEFAULT_DATETIME_FORMATTER);
        String finishedAtStr = now.format(DEFAULT_DATETIME_FORMATTER);
        String startedAtLocalStr = startedAtStr; // 테스트용으로 동일하게 설정
        String finishedAtLocalStr = finishedAtStr;
        String startedDateStr = now.minusSeconds(10).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String finishedDateStr = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String startedTimeStr = now.minusSeconds(10).format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String finishedTimeStr = now.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        String durationStr = "10초";

        // 각 템플릿 변수 값에 실제 값으로 치환
        Map<String, String> renderedVars = new HashMap<>();
        for (Map.Entry<String, String> entry : templateVars.entrySet()) {
            String varName = entry.getKey();
            String varTemplate = entry.getValue();
            
            // 변수 템플릿에 실제 값 치환
            String rendered = varTemplate;
            rendered = rendered.replace("${ruleName}", safe(rule.getName()));
            rendered = rendered.replace("${ruleId}", safeNum(rule.getId()));
            rendered = rendered.replace("${serverId}", safeNum(rule.getServerId()));
            rendered = rendered.replace("${serverName}", safe(serverName));
            rendered = rendered.replace("${serverHost}", safe(serverHost));
            rendered = rendered.replace("${serverTimezone}", safe(serverTimezone));
            rendered = rendered.replace("${outputNum}", outputNum != null ? String.valueOf(outputNum) : "");
            rendered = rendered.replace("${outputLen}", String.valueOf(outputLen));
            rendered = rendered.replace("${threshold}", threshold);
            rendered = rendered.replace("${thresholdNum}", rule.getThresholdNum() != null ? String.valueOf(rule.getThresholdNum()) : "");
            rendered = rendered.replace("${thresholdLen}", rule.getThresholdLen() != null ? String.valueOf(rule.getThresholdLen()) : "");
            rendered = rendered.replace("${pattern}", safe(rule.getPattern()));
            rendered = rendered.replace("${status}", status);
            rendered = rendered.replace("${success}", "false");
            rendered = rendered.replace("${output}", "테스트 출력값");
            rendered = rendered.replace("${error}", "");
            rendered = rendered.replace("${startedAt}", startedAtStr);
            rendered = rendered.replace("${finishedAt}", finishedAtStr);
            rendered = rendered.replace("${startedAtLocal}", startedAtLocalStr);
            rendered = rendered.replace("${finishedAtLocal}", finishedAtLocalStr);
            rendered = rendered.replace("${startedDate}", startedDateStr);
            rendered = rendered.replace("${finishedDate}", finishedDateStr);
            rendered = rendered.replace("${startedTime}", startedTimeStr);
            rendered = rendered.replace("${finishedTime}", finishedTimeStr);
            rendered = rendered.replace("${durationMs}", durationStr);
            rendered = rendered.replace("${duration}", durationStr);
            rendered = rendered.replace("${monitoringType}", rule.getMonitoringType() != null ? rule.getMonitoringType().name() : "");
            rendered = rendered.replace("${alertOperator}", rule.getAlertOperator() != null ? rule.getAlertOperator().name() : "");
            
            renderedVars.put(varName, rendered);
        }

        // JSON 형식으로 반환하여 KakaoDeliverer에서 파싱 가능하도록
        return objectMapper.writeValueAsString(renderedVars);
    }
    
    private String safe(Object obj) {
        return obj != null ? String.valueOf(obj) : "";
    }
    
    private String safeNum(Object obj) {
        return obj != null ? String.valueOf(obj) : "";
    }

    /**
     * 카카오 알림톡 테스트 발송 (템플릿 메시지 형태와 버튼 정보 사용)
     * @param receiver 수신자 전화번호
     * @param title 제목
     * @param message 메시지 (템플릿 메시지 형태에서 변수 치환된 최종 메시지)
     * @param templateCode 템플릿 코드
     * @param buttonInfo 버튼 정보 (JSON 형식)
     */
    private TestResult sendKakaoTestWithTemplate(String receiver, String title, String message, String templateCode, String buttonInfo) {
        if (!isAlimtalkAvailable()) {
            return TestResult.fail("알림톡 설정이 완료되지 않았습니다. (apiKey, userId, sender, senderKey, templateCode 필요)");
        }

        // 템플릿 코드 결정: 파라미터로 받은 것이 있으면 사용, 없으면 기본값
        String tplCode = (templateCode != null && !templateCode.trim().isEmpty()) ? templateCode : this.templateCode;
        if (tplCode == null || tplCode.trim().isEmpty()) {
            return TestResult.fail("템플릿 코드가 설정되지 않았습니다. 알림 규칙에 카카오 템플릿 ID를 설정하거나 Aligo 기본 템플릿 코드를 설정하세요.");
        }

        // 템플릿 코드 길이 검증 (Aligo API 제한: 7바이트)
        int byteLength = tplCode.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
        if (byteLength > 7) {
            return TestResult.fail("템플릿 코드는 7바이트 이하여야 합니다. 현재: " + byteLength + "바이트 (Aligo API 제한). 템플릿 코드: " + tplCode);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("apikey", apiKey);
            params.add("userid", userId);
            params.add("senderkey", senderKey);
            params.add("tpl_code", tplCode);
            params.add("sender", sender);
            params.add("receiver_1", normalizePhone(receiver));
            params.add("subject_1", truncate(title, 50));

            // 알림 내용 추출 및 포맷팅
            String systemInfo = title != null ? title : "시스템";
            String alertInfo = message != null ? message : "알림 내용 없음";

            // 메시지는 이미 템플릿 메시지 형태에서 변수 치환된 최종 메시지
            params.add("message_1", truncate(message, 1000));

            // 버튼 정보가 있으면 사용, 없으면 기본 버튼
            String button1 = StringUtils.hasText(buttonInfo) ? buttonInfo 
                    : "{\"button\":[{\"name\":\"채널추가\",\"linkType\":\"AC\",\"linkTypeName\":\"채널 추가\"}]}";
            params.add("button_1", button1);

            if ("Y".equalsIgnoreCase(testMode)) {
                params.add("testMode", "Y");
                log.warn("알림톡 테스트 모드: 실제 발송되지 않습니다. receiver={}", receiver);
            }

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(ALIMTALK_URL, request, String.class);

            String responseBody = response.getBody();
            log.info("알림톡 테스트 발송 응답: {}", responseBody);

            // Aligo API 응답 파싱 (code: 0이 성공)
            AligoResponseResult result = parseAlimtalkResponse(responseBody);
            if (result.success) {
                return TestResult.success("알림톡 테스트 발송 성공! 수신자: " + receiver + 
                        (isTestMode() ? " (테스트 모드)" : "") + 
                        (result.message != null ? "\n" + result.message : ""));
            } else {
                return TestResult.fail("알림톡 발송 실패: " + (result.message != null ? result.message : responseBody));
            }
        } catch (Exception e) {
            log.error("알림톡 테스트 발송 오류", e);
            return TestResult.fail("알림톡 발송 오류: " + e.getMessage());
        }
    }

    /**
     * SMS 테스트 발송
     */
    private TestResult sendSmsTest(String receiver, String title, String message) {
        if (!isSmsAvailable()) {
            return TestResult.fail("SMS 설정이 완료되지 않았습니다. (apiKey, userId, sender 필요)");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            String msg = StringUtils.hasText(title)
                    ? "[" + truncate(title, 44) + "]\n" + message
                    : message;

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("key", apiKey);
            params.add("user_id", userId);
            params.add("sender", sender);
            params.add("receiver", normalizePhone(receiver));
            params.add("msg", truncate(msg, 2000));
            params.add("msg_type", "LMS");
            params.add("title", truncate(title != null ? title : "모니터링알림", 44));
            if ("Y".equalsIgnoreCase(testMode)) {
                params.add("testmode_yn", "Y");
                log.warn("SMS 테스트 모드: 실제 발송되지 않습니다. receiver={}", receiver);
            }

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(SMS_URL, request, String.class);

            String responseBody = response.getBody();
            log.info("SMS 테스트 발송 응답: {}", responseBody);

            // Aligo SMS API 응답 파싱 (result_code > 0이 성공)
            SmsResponseResult result = parseSmsResponse(responseBody);
            if (result.success) {
                return TestResult.success("SMS 테스트 발송 성공! 수신자: " + receiver + 
                        (isTestMode() ? " (테스트 모드)" : "") + 
                        (result.message != null ? "\n" + result.message : ""));
            } else {
                return TestResult.fail("SMS 발송 실패: " + (result.message != null ? result.message : responseBody));
            }
        } catch (Exception e) {
            log.error("SMS 테스트 발송 오류", e);
            return TestResult.fail("SMS 발송 오류: " + e.getMessage());
        }
    }

    private boolean isAlimtalkAvailable() {
        return StringUtils.hasText(apiKey) && StringUtils.hasText(userId) && StringUtils.hasText(sender)
                && StringUtils.hasText(senderKey) && StringUtils.hasText(templateCode);
    }

    private boolean isSmsAvailable() {
        return StringUtils.hasText(apiKey) && StringUtils.hasText(userId) && StringUtils.hasText(sender);
    }

    private boolean isTestMode() {
        return "Y".equalsIgnoreCase(testMode);
    }

    private String normalizePhone(String phone) {
        if (phone == null) return "";
        return phone.replaceAll("[^0-9]", "");
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() > maxLen ? s.substring(0, maxLen) : s;
    }

    private String formatDateTime(OffsetDateTime dt, ZoneId targetZoneId) {
        if (dt == null) return "";
        try {
            if (targetZoneId != null) {
                return dt.atZoneSameInstant(targetZoneId).format(DEFAULT_DATETIME_FORMATTER);
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

    private String formatDate(OffsetDateTime dt, ZoneId targetZoneId) {
        if (dt == null) return "";
        try {
            if (targetZoneId != null) {
                return dt.atZoneSameInstant(targetZoneId).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            }
            return dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            return dt.toLocalDate().toString();
        }
    }

    private String formatTime(OffsetDateTime dt, ZoneId targetZoneId) {
        if (dt == null) return "";
        try {
            if (targetZoneId != null) {
                return dt.atZoneSameInstant(targetZoneId).format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            }
            return dt.format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        } catch (Exception e) {
            return dt.toLocalTime().toString();
        }
    }

    /**
     * Aligo 알림톡 API 응답 파싱
     */
    private AligoResponseResult parseAlimtalkResponse(String body) {
        try {
            if (body == null || body.trim().isEmpty()) {
                return new AligoResponseResult(false, "응답이 비어있습니다.");
            }
            
            log.info("Aligo 알림톡 API 응답 (전체): {}", body);
            JsonNode root = objectMapper.readTree(body);
            int code = root.path("code").asInt(1);
            String message = root.path("message").asText("");
            
            // info 필드에서 실제 발송 상태 확인
            JsonNode info = root.path("info");
            if (!info.isMissingNode()) {
                String type = info.path("type").asText("");
                long mid = info.path("mid").asLong(0);
                int scnt = info.path("scnt").asInt(0);  // 성공 카운트
                int fcnt = info.path("fcnt").asInt(0);  // 실패 카운트
                
                log.info("Aligo 알림톡 발송 상태: type={}, mid={}, scnt={}, fcnt={}", type, mid, scnt, fcnt);
                
                // fcnt가 0보다 크면 일부 또는 전체 실패
                if (fcnt > 0) {
                    log.warn("Aligo 알림톡 발송 실패 발생. scnt={}, fcnt={}, message={}", scnt, fcnt, message);
                    return new AligoResponseResult(false, "발송 실패: scnt=" + scnt + ", fcnt=" + fcnt + ", " + message);
                }
            }

            if (code == 0) {
                // 알림톡 발송 성공
                log.info("Aligo 알림톡 발송 성공. code={}, message={}", code, message);
                return new AligoResponseResult(true, message);
            }
            
            log.warn("Aligo 알림톡 발송 실패. code={}, message={}, response={}", code, message, body);
            return new AligoResponseResult(false, "code=" + code + ", " + message);
        } catch (Exception e) {
            log.error("Aligo 알림톡 응답 파싱 실패. body={}", body, e);
            return new AligoResponseResult(false, "응답 파싱 실패: " + e.getMessage());
        }
    }

    /**
     * Aligo SMS/LMS API 응답 파싱
     */
    private SmsResponseResult parseSmsResponse(String body) {
        try {
            if (body == null || body.trim().isEmpty()) {
                return new SmsResponseResult(false, "응답이 비어있습니다.");
            }
            
            log.info("Aligo SMS/LMS 응답: {}", body);
            JsonNode root = objectMapper.readTree(body);
            int resultCode = root.path("result_code").asInt(-1);
            String message = root.path("message").asText("");

            // Aligo API: result_code > 0이면 성공 (메시지 ID), < 0이면 실패 (에러 코드)
            if (resultCode > 0) {
                log.info("Aligo SMS/LMS 발송 성공. result_code={}, message={}", resultCode, message);
                return new SmsResponseResult(true, message);
            }
            
            log.warn("Aligo SMS/LMS 발송 실패. result_code={}, message={}, response={}", resultCode, message, body);
            return new SmsResponseResult(false, "result_code=" + resultCode + ", " + message);
        } catch (Exception e) {
            log.error("Aligo SMS/LMS 응답 파싱 실패. body={}", body, e);
            return new SmsResponseResult(false, "응답 파싱 실패: " + e.getMessage());
        }
    }

    /**
     * Aligo 알림톡 응답 결과
     */
    private static class AligoResponseResult {
        final boolean success;
        final String message;

        AligoResponseResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    /**
     * Aligo SMS 응답 결과
     */
    private static class SmsResponseResult {
        final boolean success;
        final String message;

        SmsResponseResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }
}
