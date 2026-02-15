package com.example.monitoring.worker.vpn;

import com.example.monitoring.common.domain.AlertRecipientEntity;
import com.example.monitoring.common.domain.CheckRunEntity;
import com.example.monitoring.common.domain.NotificationOutboxEntity;
import com.example.monitoring.common.domain.NotificationChannel;
import com.example.monitoring.common.domain.NotificationStatus;
import com.example.monitoring.common.domain.ServerStatus;
import com.example.monitoring.common.domain.VpnConnectionEntity;
import com.example.monitoring.common.domain.VpnNotificationTemplateEntity;
import com.example.monitoring.common.domain.VpnRecipientLinkEntity;
import com.example.monitoring.common.repo.CheckRunRepository;
import com.example.monitoring.common.repo.NotificationOutboxRepository;
import com.example.monitoring.common.repo.VpnNotificationTemplateRepository;
import com.example.monitoring.common.repo.VpnRecipientLinkRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * VPN 상태 변경 알림 발송
 */
@Service
@Transactional
public class VpnStatusChangeNotifier {

    private static final Logger log = LoggerFactory.getLogger(VpnStatusChangeNotifier.class);
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final NotificationOutboxRepository outboxRepo;
    private final VpnNotificationTemplateRepository templateRepo;
    private final VpnRecipientLinkRepository vpnRecipientLinkRepo;
    private final CheckRunRepository checkRunRepo;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public VpnStatusChangeNotifier(NotificationOutboxRepository outboxRepo,
                                  VpnNotificationTemplateRepository templateRepo,
                                  VpnRecipientLinkRepository vpnRecipientLinkRepo,
                                  CheckRunRepository checkRunRepo) {
        this.outboxRepo = outboxRepo;
        this.templateRepo = templateRepo;
        this.vpnRecipientLinkRepo = vpnRecipientLinkRepo;
        this.checkRunRepo = checkRunRepo;
    }

    /**
     * VPN 상태 변경 알림 발송
     */
    public void notifyStatusChange(VpnConnectionEntity vpn, ServerStatus oldStatus, ServerStatus newStatus) {
        // 유효성 검사
        if (oldStatus == newStatus) {
            log.debug("Skipping notification: status unchanged. vpn={}, status={}", vpn.getName(), oldStatus);
            return;
        }
        
        if (oldStatus == null) {
            log.warn("Skipping notification: oldStatus is null. vpn={}, newStatus={}", vpn.getName(), newStatus);
            return;
        }
        
        if (newStatus == null) {
            log.warn("Skipping notification: newStatus is null. vpn={}, oldStatus={}", vpn.getName(), oldStatus);
            return;
        }

        // 한국 시간(KST, UTC+9)으로 저장
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.of("+09:00"));
        String changeTimeStr = now.format(DATETIME_FORMATTER);
        
        // VPN 템플릿 조회 (활성화된 템플릿 중 첫 번째 사용)
        List<VpnNotificationTemplateEntity> templates = templateRepo.findByVpnIdAndEnabledTrueOrderByNameAsc(vpn.getId());
        VpnNotificationTemplateEntity template = templates.isEmpty() ? null : templates.get(0);
        
        String title;
        String body;
        
        if (template != null) {
            // 템플릿 사용
            title = renderTemplate(template.getTitleTemplate(), vpn, oldStatus, newStatus, changeTimeStr);
            body = renderTemplate(template.getBodyTemplate(), vpn, oldStatus, newStatus, changeTimeStr);
            log.info("VPN status change notification using template: vpn={}, template={}, {} -> {}", 
                    vpn.getName(), template.getName(), oldStatus, newStatus);
        } else {
            // 기본 템플릿 사용
            title = String.format("VPN 상태 변경: %s", vpn.getName() != null ? vpn.getName() : "Unknown");
            body = String.format(
                "[VPN 상태 변경 알림]\n" +
                "VPN명: %s\n" +
                "호스트: %s\n" +
                "이전 상태: %s\n" +
                "현재 상태: %s\n" +
                "변경 시간: %s",
                vpn.getName() != null ? vpn.getName() : "Unknown",
                vpn.getHost() != null ? vpn.getHost() : "Unknown",
                oldStatus != null ? oldStatus.name() : "UNKNOWN",
                newStatus != null ? newStatus.name() : "UNKNOWN",
                changeTimeStr
            );
            log.info("VPN status change notification (default template): vpn={}, {} -> {}", 
                    vpn.getName(), oldStatus, newStatus);
        }

        // 1. 체크 실행 내역에 기록 (규칙명 "VPN 상태변경", 서버명 = VPN명으로 표시되도록 vpn_id만 저장)
        CheckRunEntity checkRun = new CheckRunEntity();
        checkRun.setMonitoringRuleId(null);
        checkRun.setVpnId(vpn.getId());
        checkRun.setSuccess(true);
        checkRun.setStartedAt(now);
        checkRun.setFinishedAt(now);
        checkRun.setDurationMs(0L);
        checkRun.setOutput(body);
        checkRun.setErrorMessage(null);
        checkRun = checkRunRepo.save(checkRun);
        log.info("VPN status change check run created: vpnId={}, runId={}", vpn.getId(), checkRun.getId());

        // 2. 해당 VPN에 연결된 수신자만 조회 (VPN별 수신자 연결)
        List<VpnRecipientLinkEntity> links = vpnRecipientLinkRepo.findByVpnIdAndEnabledTrue(vpn.getId());
        if (links.isEmpty()) {
            log.warn("No enabled recipients linked for VPN. vpnId={}, name={}. VPN 알림은 연결된 수신자에게만 발송됩니다.", vpn.getId(), vpn.getName());
            return;
        }

        // 3. 각 수신자에게 알림 발송 (check_run_id 연결)
        for (VpnRecipientLinkEntity link : links) {
            AlertRecipientEntity recipient = link.getRecipient();
            if (recipient == null || !Boolean.TRUE.equals(recipient.getEnabled())) {
                continue;
            }

            String channels = recipient.getChannels();
            if (!StringUtils.hasText(channels)) {
                continue;
            }

            String[] channelArray = channels.split(",");
            for (String channelStr : channelArray) {
                String ch = channelStr.trim().toUpperCase();
                NotificationChannel channel = parseChannel(ch);
                if (channel == null) continue;

                String toAddr = getRecipientAddress(recipient, channel);
                if (!StringUtils.hasText(toAddr)) continue;

                // KAKAO 채널이고 VPN에 카카오 템플릿 변수가 있으면 알림규칙과 동일하게 JSON body 사용
                String notificationTitle = title;
                String notificationBody = body;
                if (channel == NotificationChannel.KAKAO && StringUtils.hasText(vpn.getKakaoTemplateCode()) && StringUtils.hasText(vpn.getKakaoTemplateVariables())) {
                    try {
                        notificationBody = buildVpnKakaoTemplateBody(vpn, oldStatus, newStatus, changeTimeStr);
                        notificationTitle = "VPN 상태 변경: " + (vpn.getName() != null ? vpn.getName() : "Unknown");
                        log.info("VPN KAKAO 알림: 카카오 템플릿 변수 사용. vpn={}", vpn.getName());
                    } catch (Exception e) {
                        log.warn("VPN KAKAO 템플릿 변수 body 생성 실패, 기본 body 사용: vpn={}, error={}", vpn.getName(), e.getMessage());
                    }
                }
                createNotification(toAddr, channel, notificationTitle, notificationBody, now, checkRun.getId(), vpn);
            }
        }
    }

    /**
     * 알림 생성 및 큐에 추가 (체크 실행 내역과 연결)
     */
    private void createNotification(String toAddr, NotificationChannel channel,
                                   String title, String body, OffsetDateTime now, Long checkRunId, VpnConnectionEntity vpn) {
        NotificationOutboxEntity notification = new NotificationOutboxEntity();
        notification.setStatus(NotificationStatus.PENDING);
        notification.setChannel(channel);
        notification.setMonitoringRuleId(null);
        notification.setCheckRunId(checkRunId);
        notification.setToAddr(toAddr);
        notification.setTitle(title);
        notification.setBody(body);
        // KAKAO 채널인 경우 VPN의 템플릿 코드 저장
        if (channel == NotificationChannel.KAKAO && vpn != null) {
            notification.setKakaoTemplateCode(vpn.getKakaoTemplateCode());
            log.info("VPN status change notification created: to={}, channel={}, checkRunId={}, kakaoTemplateCode={}", 
                    toAddr, channel, checkRunId, vpn.getKakaoTemplateCode());
        } else {
            log.info("VPN status change notification created: to={}, channel={}, checkRunId={}, title={}", 
                    toAddr, channel, checkRunId, title);
        }
        notification.setCreatedAt(now);
        notification.setNextAttemptAt(now);
        notification.setAttempt(0);
        notification.setMaxAttempt(5);

        outboxRepo.save(notification);
    }

    /**
     * 채널 문자열을 NotificationChannel enum으로 변환
     */
    private NotificationChannel parseChannel(String ch) {
        try {
            return NotificationChannel.valueOf(ch);
        } catch (Exception e) {
            log.warn("Invalid notification channel: {}", ch);
            return null;
        }
    }

    /**
     * 수신자의 채널에 맞는 주소 반환
     */
    private String getRecipientAddress(AlertRecipientEntity recipient, NotificationChannel channel) {
        return switch (channel) {
            case SMS -> recipient.getPhone();
            case EMAIL -> recipient.getEmail();
            case KAKAO -> recipient.getKakao();
        };
    }

    /**
     * VPN 카카오 템플릿 변수 JSON 생성 (알림규칙과 동일 방식)
     * vpn.kakaoTemplateVariables(JSON) 각 값에 ${vpnName}, ${vpnHost} 등 치환 후 JSON 문자열 반환
     */
    private String buildVpnKakaoTemplateBody(VpnConnectionEntity vpn, ServerStatus oldStatus, ServerStatus newStatus, String changeTimeStr) throws Exception {
        String templateVariablesJson = vpn.getKakaoTemplateVariables();
        if (!StringUtils.hasText(templateVariablesJson)) {
            throw new IllegalArgumentException("kakaoTemplateVariables is empty");
        }
        @SuppressWarnings("unchecked")
        Map<String, String> templateVars = objectMapper.readValue(templateVariablesJson,
                objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, String.class));
        Map<String, String> renderedVars = new HashMap<>();
        for (Map.Entry<String, String> entry : templateVars.entrySet()) {
            String varName = entry.getKey();
            String varTemplate = entry.getValue();
            String rendered = renderTemplate(varTemplate != null ? varTemplate : "", vpn, oldStatus, newStatus, changeTimeStr);
            renderedVars.put(varName, rendered);
        }
        return objectMapper.writeValueAsString(renderedVars);
    }

    /**
     * 템플릿 변수 치환
     * 줄바꿈 문자(\n, \r\n)는 그대로 유지하여 알림 전송 시 줄바꿈이 보존되도록 함
     */
    private String renderTemplate(String template, VpnConnectionEntity vpn, 
                                 ServerStatus oldStatus, ServerStatus newStatus, 
                                 String changeTime) {
        if (template == null) {
            return "";
        }
        
        // 변수 값 검증 및 안전한 기본값 설정
        String vpnName = (vpn != null && vpn.getName() != null) ? vpn.getName() : "알수없음";
        String vpnHost = (vpn != null && vpn.getHost() != null) ? vpn.getHost() : "알수없음";
        String oldStatusStr = (oldStatus != null) ? oldStatus.name() : "알수없음";
        String newStatusStr = (newStatus != null) ? newStatus.name() : "알수없음";
        String changeTimeStr = (changeTime != null && !changeTime.isEmpty()) ? changeTime : "알수없음";
        
        // 템플릿 변수 치환 (줄바꿈 문자는 그대로 유지)
        String result = template
                .replace("${vpnName}", vpnName)
                .replace("${vpnHost}", vpnHost)
                .replace("${oldStatus}", oldStatusStr)
                .replace("${newStatus}", newStatusStr)
                .replace("${changeTime}", changeTimeStr);
        
        // 줄바꿈 문자 보존: \r\n을 \n으로 정규화하지 않고 그대로 유지
        // (일부 시스템에서 \r\n을 사용하더라도 Aligo API가 처리할 수 있도록)
        return result;
    }
}
