package com.example.monitoring.worker.alert;

import com.example.monitoring.common.domain.NotificationChannel;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class DelivererRouter {

    private final Map<NotificationChannel, NotificationDeliverer> map = new EnumMap<>(NotificationChannel.class);

    public DelivererRouter(List<NotificationDeliverer> deliverers) {
        for (NotificationDeliverer d : deliverers) {
            map.put(d.channel(), d);
        }
    }

    public NotificationDeliverer route(NotificationChannel channel) {
        NotificationDeliverer d = map.get(channel);
        if (d == null) throw new IllegalStateException("No deliverer for channel=" + channel);
        return d;
    }
}