package com.example.monitoring.worker.alert;

import com.example.monitoring.common.domain.NotificationChannel;
import com.example.monitoring.common.domain.NotificationOutboxEntity;

public interface NotificationProvider {
    NotificationChannel channel();
    void send(NotificationOutboxEntity n) throws Exception;
}