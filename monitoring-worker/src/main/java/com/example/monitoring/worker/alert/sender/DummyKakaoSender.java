package com.example.monitoring.worker.alert.sender;

import com.example.monitoring.worker.alert.ChannelSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class DummyKakaoSender implements ChannelSender {

    private static final Logger log = LoggerFactory.getLogger(DummyKakaoSender.class);

    @Override
    public boolean supports(String channel) {
        return "KAKAO".equalsIgnoreCase(channel);
    }

    @Override
    public void send(String to, String title, String body) {
        // TODO: 카카오 채널/알림톡 API 연동 위치
        // to = 채널 식별자 or 수신자 key (지금은 문자열로 유지)
        log.info("[DUMMY-KAKAO] to={}, title={}, body={}", to, title, body);
    }
}