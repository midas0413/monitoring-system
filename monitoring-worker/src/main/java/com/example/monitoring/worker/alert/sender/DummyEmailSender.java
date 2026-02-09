package com.example.monitoring.worker.alert.sender;

import com.example.monitoring.worker.alert.ChannelSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DummyEmailSender implements ChannelSender {

    private static final Logger log = LoggerFactory.getLogger(DummyEmailSender.class);

    @Override
    public boolean supports(String channel) {
        return "EMAIL".equalsIgnoreCase(channel);
    }

    @Override
    public void send(String to, String title, String body) {
        // TODO: JavaMailSender 연동 위치
        log.info("[DUMMY-EMAIL] to={}, title={}, body={}", to, title, body);
    }
}