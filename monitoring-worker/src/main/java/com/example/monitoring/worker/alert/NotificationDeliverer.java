package com.example.monitoring.worker.alert;

import com.example.monitoring.common.domain.NotificationChannel;

public interface NotificationDeliverer {

    NotificationChannel channel();

    /**
     * 실제 외부 연동(SMS/KAKAO/EMAIL)은 아직 붙이기 전 단계.
     * 여기서는 "전송 시도" 결과만 리턴.
     */
    DeliverResult deliver(String toAddr, String title, String body);

    record DeliverResult(boolean success, String providerMessage) {
        public static DeliverResult ok(String msg) { return new DeliverResult(true, msg); }
        public static DeliverResult fail(String msg) { return new DeliverResult(false, msg); }
    }
}