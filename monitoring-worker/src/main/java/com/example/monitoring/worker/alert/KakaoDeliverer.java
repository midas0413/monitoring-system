package com.example.monitoring.worker.alert;

import com.example.monitoring.common.domain.CheckRunEntity;
import com.example.monitoring.common.domain.KakaoTemplateEntity;
import com.example.monitoring.common.domain.MonitoringRuleEntity;
import com.example.monitoring.common.domain.NotificationChannel;
import com.example.monitoring.common.domain.ServerEntity;
import com.example.monitoring.common.domain.VpnConnectionEntity;
// import com.example.monitoring.common.repo.CheckRepository;  // Deprecated
import com.example.monitoring.common.repo.CheckRunRepository;
import com.example.monitoring.common.repo.KakaoTemplateRepository;
import com.example.monitoring.common.repo.MonitoringRuleRepository;
import com.example.monitoring.common.repo.ServerRepository;
import com.example.monitoring.common.repo.VpnConnectionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * KAKAO 알림 발송 - Aligo API 연동
 * 1) 알림톡 (템플릿 있으면) 2) LMS 문자 (fallback)
 */
@Component
public class KakaoDeliverer implements NotificationDeliverer {

    private static final Logger log = LoggerFactory.getLogger(KakaoDeliverer.class);

    private final AligoClient aligoClient;
    private final AligoProperties aligoProperties;
    private final CheckRunRepository checkRunRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MonitoringRuleRepository monitoringRuleRepository;
    private final ServerRepository serverRepository;
    private final KakaoTemplateRepository kakaoTemplateRepository;
    private final VpnConnectionRepository vpnConnectionRepository;

    public KakaoDeliverer(
            AligoClient aligoClient,
            AligoProperties aligoProperties,
            CheckRunRepository checkRunRepository,
            MonitoringRuleRepository monitoringRuleRepository,
            ServerRepository serverRepository,
            KakaoTemplateRepository kakaoTemplateRepository,
            VpnConnectionRepository vpnConnectionRepository
    ) {
        this.aligoClient = aligoClient;
        this.aligoProperties = aligoProperties;
        this.checkRunRepository = checkRunRepository;
        this.monitoringRuleRepository = monitoringRuleRepository;
        this.serverRepository = serverRepository;
        this.kakaoTemplateRepository = kakaoTemplateRepository;
        this.vpnConnectionRepository = vpnConnectionRepository;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.KAKAO;
    }

    @Override
    public DeliverResult deliver(String toAddr, String title, String body) {
        // 이 메서드는 NotificationOutboxEntity 없이 호출되므로 body에서만 추출
        return deliverWithContext(toAddr, title, body, null);
    }
    
    /**
     * checkRunId를 통해 정확한 서버명을 가져오는 메서드
     * @param toAddr 수신자 주소
     * @param title 제목
     * @param body 본문
     * @param checkRunId 체크 실행 ID (서버명 추출용)
     * @param kakaoTemplateCode 카카오 템플릿 코드 (null이면 AligoProperties의 기본 템플릿 코드 사용)
     */
    public DeliverResult deliverWithContext(String toAddr, String title, String body, Long checkRunId) {
        return deliverWithContext(toAddr, title, body, checkRunId, null);
    }
    
    /**
     * checkRunId와 템플릿 코드를 받아서 알림 발송
     */
    public DeliverResult deliverWithContext(String toAddr, String title, String body, Long checkRunId, String kakaoTemplateCode) {
        if (!StringUtils.hasText(toAddr)) {
            return DeliverResult.fail("수신자 정보 없음");
        }

        // 전화번호 형식이 아니면 LMS 발송 불가 (알림톡도 전화번호 필수)
        String phone = toAddr.replaceAll("[^0-9]", "");
        if (phone.length() < 10) {
            log.warn("[KAKAO] 수신자가 전화번호 형식이 아님. toAddr={}", toAddr);
            return DeliverResult.fail("KAKAO 발송은 전화번호가 필요합니다. 수신자에 전화번호를 등록하세요.");
        }

        if (!aligoProperties.isSmsAvailable()) {
            log.warn("[KAKAO] Aligo 설정이 없습니다. aligo.api-key, aligo.user-id, aligo.sender 를 설정하세요.");
            return DeliverResult.fail("Aligo 설정 부족");
        }

        // 1) 알림톡 시도 (설정 있으면)
        if (aligoProperties.isAlimtalkAvailable()) {
            // body가 JSON 형식인지 확인 (템플릿 변수 값이 있는 경우)
            Map<String, String> templateVars = null;
            String systemName = null;
            String alertContent = null;
            
            if (body != null && body.trim().startsWith("{") && body.trim().endsWith("}")) {
                try {
                    // JSON 형식으로 파싱
                    templateVars = objectMapper.readValue(body, 
                            objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, String.class));
                    log.info("[KAKAO] 템플릿 변수 JSON 파싱 성공: {}", templateVars);
                    
                    // 템플릿 변수에서 "시스템" 또는 첫 번째 변수를 systemName으로, "알림" 또는 두 번째 변수를 alertContent로 사용
                    // 변수명이 정확히 일치하지 않을 수 있으므로 순서대로 사용
                    String[] varNames = templateVars.keySet().toArray(new String[0]);
                    if (varNames.length > 0) {
                        systemName = templateVars.get(varNames[0]);
                        // "시스템"이라는 이름의 변수가 있으면 우선 사용
                        if (templateVars.containsKey("시스템")) {
                            systemName = templateVars.get("시스템");
                        }
                    }
                    if (varNames.length > 1) {
                        alertContent = templateVars.get(varNames[1]);
                        // "알림"이라는 이름의 변수가 있으면 우선 사용
                        if (templateVars.containsKey("알림")) {
                            alertContent = templateVars.get("알림");
                        }
                    } else if (varNames.length == 1) {
                        // 변수가 하나만 있으면 그것을 alertContent로 사용
                        alertContent = templateVars.get(varNames[0]);
                    }
                } catch (Exception e) {
                    log.warn("[KAKAO] 템플릿 변수 JSON 파싱 실패, 기본 방식 사용: {}", e.getMessage());
                }
            }
            
            // JSON 파싱 실패하거나 JSON이 아니면 기본 방식 사용
            if (systemName == null || alertContent == null) {
                systemName = extractSystemName(body, title, checkRunId);
                alertContent = extractAlertContent(body);
            }
            
            // 변수 값 검증
            if (!StringUtils.hasText(systemName)) {
                log.warn("[KAKAO] 시스템명이 비어있습니다. checkRunId={}, body={}", checkRunId, body);
                systemName = "알 수 없음"; // 기본값 설정
            }
            if (!StringUtils.hasText(alertContent)) {
                log.warn("[KAKAO] 알림내용이 비어있습니다. body={}", body);
                alertContent = body != null ? body : "알림 내용 없음"; // 기본값 설정
            }
            
            log.info("[KAKAO] 알림톡 템플릿 변수. var1(시스템)={}, var2(알림)={}", systemName, alertContent);
            
            // 템플릿 코드 결정: 우선순위
            // 1. 파라미터로 받은 템플릿 코드 (NotificationOutboxEntity에서 전달됨)
            // 2. 없으면 checkRunId를 통해 알림 규칙에서 가져오기 (방어적 프로그래밍)
            // 3. 그것도 없으면 AligoProperties의 기본 템플릿 코드 사용
            String templateCode = kakaoTemplateCode;
            
            if (!StringUtils.hasText(templateCode) && checkRunId != null) {
                // 파라미터로 받은 템플릿 코드가 없으면 알림 규칙에서 가져오기
                CheckRunEntity run = checkRunRepository.findById(checkRunId).orElse(null);
                if (run != null && run.getMonitoringRuleId() != null) {
                    MonitoringRuleEntity rule = monitoringRuleRepository.findById(run.getMonitoringRuleId()).orElse(null);
                    if (rule != null && StringUtils.hasText(rule.getKakaoTemplateCode())) {
                        templateCode = rule.getKakaoTemplateCode();
                        log.info("[KAKAO] 알림 규칙에서 템플릿 코드 가져옴. ruleId={}, templateCode={}", rule.getId(), templateCode);
                    }
                }
            }
            
            // 여전히 없으면 AligoProperties의 기본 템플릿 코드 사용 (기존 동작 유지)
            if (!StringUtils.hasText(templateCode)) {
                templateCode = aligoProperties.getTemplateCode();
                log.info("[KAKAO] 기본 템플릿 코드 사용. templateCode={}", templateCode);
            } else {
                log.info("[KAKAO] 알림 규칙 템플릿 코드 사용. templateCode={}", templateCode);
            }
            
            // 템플릿 엔티티 조회 (템플릿 메시지 형태와 버튼 정보 사용)
            KakaoTemplateEntity template = null;
            String templateMessage = null;
            String buttonInfo = null;
            if (StringUtils.hasText(templateCode)) {
                template = kakaoTemplateRepository.findByTemplateCode(templateCode).orElse(null);
                if (template != null) {
                    templateMessage = template.getTemplateMessage();
                    buttonInfo = template.getButtonInfo();
                    log.info("[KAKAO] 템플릿 정보 조회. templateCode={}, hasTemplateMessage={}, hasButtonInfo={}", 
                            templateCode, StringUtils.hasText(templateMessage), StringUtils.hasText(buttonInfo));
                } else {
                    log.warn("[KAKAO] 템플릿 엔티티를 찾을 수 없음. templateCode={}", templateCode);
                }
            }
            
            // 템플릿 메시지 형태가 있으면 변수 치환하여 메시지 조합
            String finalMessage = body;
            if (StringUtils.hasText(templateMessage) && templateVars != null) {
                // 템플릿 메시지 형태에서 #{변수명} 형식을 변수 값으로 치환
                finalMessage = templateMessage;
                for (Map.Entry<String, String> entry : templateVars.entrySet()) {
                    String varName = entry.getKey();
                    String varValue = entry.getValue();
                    // #{변수명} 형식을 변수 값으로 치환
                    finalMessage = finalMessage.replace("#{" + varName + "}", varValue);
                }
                log.info("[KAKAO] 템플릿 메시지 형태 사용. templateMessage={}, finalMessage={}", templateMessage, finalMessage);
            } else if (StringUtils.hasText(templateMessage)) {
                // 템플릿 변수가 JSON이 아니면 기존 방식 사용 (systemName, alertContent)
                finalMessage = templateMessage;
                if (StringUtils.hasText(systemName)) {
                    finalMessage = finalMessage.replace("#{시스템}", systemName);
                }
                if (StringUtils.hasText(alertContent)) {
                    finalMessage = finalMessage.replace("#{알림}", alertContent);
                }
                log.info("[KAKAO] 템플릿 메시지 형태 사용 (기존 변수). templateMessage={}, finalMessage={}", templateMessage, finalMessage);
            }
            
            var ar = aligoClient.sendAlimtalk(toAddr, title, finalMessage, systemName, alertContent, templateCode, buttonInfo);
            if (ar.success()) {
                // Aligo API 응답 메시지에 "대체발송" 키워드가 포함되어 있는지 확인
                String resultMsg = ar.message() != null ? ar.message() : "";
                if (resultMsg.contains("대체발송") || resultMsg.contains("failover")) {
                    log.warn("[KAKAO] 알림톡 발송 실패 후 Aligo에서 자동 대체발송(SMS/LMS) 처리됨. to={}, message={}", toAddr, resultMsg);
                } else {
                    log.info("[KAKAO] 알림톡 발송 완료. to={}, systemName={}", toAddr, systemName);
                }
                return DeliverResult.ok(ar.message());
            }
            log.warn("[KAKAO] 알림톡 실패, LMS로 재시도. err={}", ar.message());
        }

        // 2) LMS 문자 fallback
        var lr = aligoClient.sendLms(toAddr, title, body);
        if (lr.success()) {
            log.info("[KAKAO] LMS 발송 완료. to={}", toAddr);
            return DeliverResult.ok(lr.message());
        }
        return DeliverResult.fail(lr.message());
    }
    
    /**
     * 시스템명(서버명/VPN명) 추출 - 템플릿 변수 #{시스템}에 매핑
     * checkRunId가 있으면 MonitoringRuleEntity와 ServerEntity에서 정확히 가져오고, 없으면 body나 title에서 추출
     */
    private String extractSystemName(String body, String title, Long checkRunId) {
        // 1) checkRunId를 통해 정확한 서버명 또는 VPN명 가져오기
        if (checkRunId != null) {
            CheckRunEntity run = checkRunRepository.findById(checkRunId).orElse(null);
            if (run != null) {
                if (run.getMonitoringRuleId() != null) {
                    MonitoringRuleEntity rule = monitoringRuleRepository.findById(run.getMonitoringRuleId()).orElse(null);
                    if (rule != null && rule.getServerId() != null) {
                        ServerEntity server = serverRepository.findById(rule.getServerId()).orElse(null);
                        if (server != null && StringUtils.hasText(server.getName())) {
                            return server.getName();
                        }
                    }
                } else if (run.getVpnId() != null) {
                    // VPN 상태변경 알림: vpn_id로 VPN명 조회
                    VpnConnectionEntity vpn = vpnConnectionRepository.findById(run.getVpnId()).orElse(null);
                    if (vpn != null && StringUtils.hasText(vpn.getName())) {
                        return vpn.getName();
                    }
                }
            }
        }
        
        // 2) body에서 추출 시도
        String extracted = extractServerNameFromBody(body);
        if (StringUtils.hasText(extracted)) {
            return extracted;
        }
        
        // 3) title에서 추출 시도 (VPN 상태 변경: {vpnName} 형식)
        if (StringUtils.hasText(title)) {
            Pattern patternTitle = Pattern.compile("VPN\\s*상태\\s*변경\\s*[:：]\\s*([^\\s]+)", Pattern.CASE_INSENSITIVE);
            Matcher matcherTitle = patternTitle.matcher(title);
            if (matcherTitle.find()) {
                String vpnName = matcherTitle.group(1).trim();
                if (vpnName.length() <= 50) {
                    return vpnName;
                }
            }
        }
        
        // 4) 모든 추출 실패 시 빈 문자열 반환
        return "";
    }
    
    /**
     * 알림내용 추출 - 템플릿 변수 #{알림}에 매핑
     * body를 간결하게 정리하여 반환 (줄바꿈 유지)
     */
    private String extractAlertContent(String body) {
        if (!StringUtils.hasText(body)) return "";
        
        // body의 줄바꿈 유지 (템플릿의 줄바꿈을 그대로 전달)
        String content = body.trim();
        
        // body가 너무 길면 앞부분만 사용 (1000자 제한, Aligo API 제한 고려)
        if (content.length() > 1000) {
            // 줄바꿈을 기준으로 잘라서 마지막 줄이 잘리지 않도록 처리
            String[] lines = content.split("\n");
            StringBuilder sb = new StringBuilder();
            int totalLength = 0;
            for (String line : lines) {
                if (totalLength + line.length() + 1 > 1000) {
                    break;
                }
                if (sb.length() > 0) {
                    sb.append("\n");
                    totalLength++;
                }
                sb.append(line);
                totalLength += line.length();
            }
            if (sb.length() < content.length()) {
                sb.append("\n...");
            }
            content = sb.toString();
        }
        
        // 줄바꿈은 유지 (공백으로 변환하지 않음)
        return content;
    }

    /**
     * body에서 서버명/VPN명 추출
     * body에 target=, serverName=, targetName=, VPN명: 등의 패턴이 있으면 추출
     * 없으면 body에서 첫 번째 서버명 패턴을 찾거나, 빈 문자열 반환
     */
    private String extractServerNameFromBody(String body) {
        if (!StringUtils.hasText(body)) return "";

        // 패턴 1: target=서버명, serverName=서버명, targetName=서버명
        Pattern pattern1 = Pattern.compile("(?:target|serverName|targetName)\\s*[=:]\\s*([^\\s\\n,]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher1 = pattern1.matcher(body);
        if (matcher1.find()) {
            return matcher1.group(1).trim();
        }

        // 패턴 2: VPN명: {vpnName} (VPN 상태 변경 알림용)
        Pattern patternVpn = Pattern.compile("VPN명\\s*[:：]\\s*([^\\s\\n]+)", Pattern.CASE_INSENSITIVE);
        Matcher matcherVpn = patternVpn.matcher(body);
        if (matcherVpn.find()) {
            String vpnName = matcherVpn.group(1).trim();
            if (vpnName.length() <= 50) {
                return vpnName;
            }
        }

        // 패턴 3: [서버명] 형식
        Pattern pattern2 = Pattern.compile("\\[([^\\]]+)\\]");
        Matcher matcher2 = pattern2.matcher(body);
        if (matcher2.find()) {
            String found = matcher2.group(1).trim();
            // 너무 긴 경우 제외 (50자 이상)
            if (found.length() <= 50) {
                return found;
            }
        }

        // 패턴 4: ${targetName}, ${serverName} 등이 치환된 경우를 찾기 어려우므로 빈 문자열 반환
        // 실제로는 NotificationOutboxEntity의 checkRunId를 통해 조회하는 것이 더 정확하지만,
        // 현재 구조상 body만 받으므로 추출 로직으로 처리
        return "";
    }
}