package com.example.monitoring.worker.alert.sender;

import com.example.monitoring.common.domain.NotificationChannel;
import com.example.monitoring.common.domain.NotificationOutboxEntity;
import com.example.monitoring.worker.alert.NotificationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DummySmsSender implements NotificationProvider {
    private static final Logger log = LoggerFactory.getLogger(DummySmsSender.class);

    @Override public NotificationChannel channel() { return NotificationChannel.SMS; }

    @Override
    public void send(NotificationOutboxEntity n) {
        log.info("[SMS] to={} title={} body={}", n.getToAddr(), n.getTitle(), n.getBody());
    }
}