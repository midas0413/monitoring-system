package com.example.monitoring.worker.vpn;

import com.example.monitoring.common.domain.AlertRecipientEntity;
import com.example.monitoring.common.domain.NotificationOutboxEntity;
import com.example.monitoring.common.domain.NotificationChannel;
import com.example.monitoring.common.domain.NotificationStatus;
import com.example.monitoring.common.domain.ServerStatus;
import com.example.monitoring.common.domain.VpnConnectionEntity;
import com.example.monitoring.common.repo.AlertRecipientRepository;
import com.example.monitoring.common.repo.NotificationOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * VPN 상태 변경 알림 발송
 */
@Service
@Transactional
public class VpnStatusChangeNotifier {

    private static final Logger log = LoggerFactory.getLogger(VpnStatusChangeNotifier.class);
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final NotificationOutboxRepository outboxRepo;
    private final AlertRecipientRepository recipientRepo;

    public VpnStatusChangeNotifier(NotificationOutboxRepository outboxRepo,
                                  AlertRecipientRepository recipientRepo) {
        this.outboxRepo = outboxRepo;
        this.recipientRepo = recipientRepo;
    }

    /**
     * VPN 상태 변경 알림 발송
     */
    public void notifyStatusChange(VpnConnectionEntity vpn, ServerStatus oldStatus, ServerStatus newStatus) {
        if (oldStatus == newStatus) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        String title = String.format("VPN 상태 변경: %s", vpn.getName());
        String body = String.format(
            "[VPN 상태 변경 알림]\n" +
            "VPN명: %s\n" +
            "호스트: %s\n" +
            "이전 상태: %s\n" +
            "현재 상태: %s\n" +
            "변경 시간: %s",
            vpn.getName(),
            vpn.getHost(),
            oldStatus,
            newStatus,
            now.format(DATETIME_FORMATTER)
        );

        log.info("VPN status change notification: vpn={}, {} -> {}", 
                vpn.getName(), oldStatus, newStatus);

        // 활성화된 수신자 목록 가져오기
        List<AlertRecipientEntity> recipients = recipientRepo.findByEnabledTrue();
        
        if (recipients.isEmpty()) {
            log.warn("No enabled recipients found for VPN status change notification");
            return;
        }

        // 각 수신자에게 알림 발송
        for (AlertRecipientEntity recipient : recipients) {
            if (!recipient.getEnabled()) {
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

                createNotification(toAddr, channel, title, body, now);
            }
        }
    }

    /**
     * 알림 생성 및 큐에 추가
     */
    private void createNotification(String toAddr, NotificationChannel channel, 
                                   String title, String body, OffsetDateTime now) {
        NotificationOutboxEntity notification = new NotificationOutboxEntity();
        notification.setStatus(NotificationStatus.PENDING);
        notification.setChannel(channel);
        notification.setToAddr(toAddr);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setCreatedAt(now);
        notification.setNextAttemptAt(now);

        outboxRepo.save(notification);
        log.info("VPN status change notification created: to={}, channel={}", toAddr, channel);
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
}
