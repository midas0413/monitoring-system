package com.example.monitoring.worker.alert;

import com.example.monitoring.common.domain.NotificationChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DefaultNotificationDeliverer implements NotificationDeliverer {

    private static final Logger log = LoggerFactory.getLogger(DefaultNotificationDeliverer.class);

    @Override
    public NotificationChannel channel() {
        // 기본 deliverer는 직접 라우팅에 안 넣거나,
        // 혹시 넣더라도 특정 채널 하나로만 쓰는 게 안전함.
        // 여기서는 EMAIL로 두되 실제 라우터에서 못 찾으면 이걸 쓰는 방식 추천.
        return NotificationChannel.EMAIL;
    }

    @Override
    public DeliverResult deliver(String toAddr, String title, String body) {
        log.info("[DEFAULT DELIVER] to={}, title={}, body={}", toAddr, title, body);
        return DeliverResult.ok("default deliver (no real api)");
    }
}