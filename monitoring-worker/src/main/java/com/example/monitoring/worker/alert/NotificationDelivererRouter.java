package com.example.monitoring.worker.alert;

import com.example.monitoring.common.domain.NotificationChannel;
import org.springframework.stereotype.Component;

@Component
public class NotificationDelivererRouter {

    private final SmsDeliverer sms;
    private final EmailDeliverer email;
    private final KakaoDeliverer kakao;

    public NotificationDelivererRouter(SmsDeliverer sms, EmailDeliverer email, KakaoDeliverer kakao) {
        this.sms = sms;
        this.email = email;
        this.kakao = kakao;
    }

    public NotificationDeliverer route(NotificationChannel ch) {
        return switch (ch) {
            case SMS -> sms;
            case EMAIL -> email;
            case KAKAO -> kakao;
        };
    }
}