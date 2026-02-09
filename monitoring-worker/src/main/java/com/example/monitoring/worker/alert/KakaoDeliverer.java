package com.example.monitoring.worker.alert;

import com.example.monitoring.common.domain.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * KAKAO 알림 발송 - Aligo API 연동
 * 1) 알림톡 (템플릿 있으면) 2) LMS 문자 (fallback)
 */
@Component
public class KakaoDeliverer implements NotificationDeliverer {

    private static final Logger log = LoggerFactory.getLogger(KakaoDeliverer.class);

    private final AligoClient aligoClient;
    private final AligoProperties aligoProperties;

    public KakaoDeliverer(AligoClient aligoClient, AligoProperties aligoProperties) {
        this.aligoClient = aligoClient;
        this.aligoProperties = aligoProperties;
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.KAKAO;
    }

    @Override
    public DeliverResult deliver(String toAddr, String title, String body) {
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
            var ar = aligoClient.sendAlimtalk(toAddr, title, body);
            if (ar.success()) {
                log.info("[KAKAO] 알림톡 발송 완료. to={}", toAddr);
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
}