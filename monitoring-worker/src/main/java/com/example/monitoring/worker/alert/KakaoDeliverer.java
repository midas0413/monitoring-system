package com.example.monitoring.worker.alert;

import com.example.monitoring.common.domain.CheckRunEntity;
import com.example.monitoring.common.domain.MonitoringRuleEntity;
import com.example.monitoring.common.domain.NotificationChannel;
import com.example.monitoring.common.domain.ServerEntity;
// import com.example.monitoring.common.repo.CheckRepository;  // Deprecated
import com.example.monitoring.common.repo.CheckRunRepository;
import com.example.monitoring.common.repo.MonitoringRuleRepository;
import com.example.monitoring.common.repo.ServerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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
    private final MonitoringRuleRepository monitoringRuleRepository;
    private final ServerRepository serverRepository;

    public KakaoDeliverer(
            AligoClient aligoClient,
            AligoProperties aligoProperties,
            CheckRunRepository checkRunRepository,
            MonitoringRuleRepository monitoringRuleRepository,
            ServerRepository serverRepository
    ) {
        this.aligoClient = aligoClient;
        this.aligoProperties = aligoProperties;
        this.checkRunRepository = checkRunRepository;
        this.monitoringRuleRepository = monitoringRuleRepository;
        this.serverRepository = serverRepository;
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
     */
    public DeliverResult deliverWithContext(String toAddr, String title, String body, Long checkRunId) {
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
            // 템플릿 변수 추출: var1=#{시스템}(서버명/VPN명), var2=#{알림}(알림내용)
            String systemName = extractSystemName(body, title, checkRunId);
            String alertContent = extractAlertContent(body);
            
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
            
            var ar = aligoClient.sendAlimtalk(toAddr, title, body, systemName, alertContent);
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
        // 1) checkRunId를 통해 정확한 서버명 가져오기
        if (checkRunId != null) {
            CheckRunEntity run = checkRunRepository.findById(checkRunId).orElse(null);
            if (run != null && run.getMonitoringRuleId() != null) {
                MonitoringRuleEntity rule = monitoringRuleRepository.findById(run.getMonitoringRuleId()).orElse(null);
                if (rule != null && rule.getServerId() != null) {
                    ServerEntity server = serverRepository.findById(rule.getServerId()).orElse(null);
                    if (server != null && StringUtils.hasText(server.getName())) {
                        return server.getName();
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
     * body를 간결하게 정리하여 반환
     */
    private String extractAlertContent(String body) {
        if (!StringUtils.hasText(body)) return "";
        
        // body가 너무 길면 앞부분만 사용 (200자 제한)
        String content = body.trim();
        if (content.length() > 200) {
            content = content.substring(0, 197) + "...";
        }
        
        // 줄바꿈을 공백으로 변환 (템플릿 형식에 맞게)
        content = content.replaceAll("\\s+", " ");
        
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