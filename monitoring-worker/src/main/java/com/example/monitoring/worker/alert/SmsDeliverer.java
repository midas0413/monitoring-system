package com.example.monitoring.worker.alert;

import com.example.monitoring.common.domain.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * SMS 알림 발송 - Aligo API 연동
 * 90byte 이하면 단문(SMS), 초과 시 장문(LMS)으로 자동 발송
 */
@Component
public class SmsDeliverer implements NotificationDeliverer {

    private static final Logger log = LoggerFactory.getLogger(SmsDeliverer.class);

    private final AligoClient aligoClient;
    private final AligoProperties aligoProperties;

    public SmsDeliverer(AligoClient aligoClient, AligoProperties aligoProperties) {
        this.aligoClient = aligoClient;
        this.aligoProperties = aligoProperties;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SMS;
    }

    @Override
    public DeliverResult deliver(String toAddr, String title, String body) {
        if (!StringUtils.hasText(toAddr)) {
            return DeliverResult.fail("수신자 정보 없음");
        }

        String phone = toAddr.replaceAll("[^0-9]", "");
        if (phone.length() < 10) {
            log.warn("[SMS] 수신자가 전화번호 형식이 아님. toAddr={}", toAddr);
            return DeliverResult.fail("SMS 발송은 전화번호가 필요합니다.");
        }

        if (!aligoProperties.isSmsAvailable()) {
            log.warn("[SMS] Aligo 설정이 없습니다. aligo.api-key, aligo.user-id, aligo.sender 를 설정하세요.");
            return DeliverResult.fail("Aligo 설정 부족");
        }

        var result = aligoClient.sendSms(toAddr, title, body);
        if (result.success()) {
            log.info("[SMS] 발송 완료. to={}", toAddr);
            return DeliverResult.ok(result.message());
        }
        return DeliverResult.fail(result.message());
    }
}