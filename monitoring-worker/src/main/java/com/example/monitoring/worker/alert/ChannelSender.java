package com.example.monitoring.worker.alert;

public interface ChannelSender {
    boolean supports(String channel); // "SMS" / "KAKAO" / "EMAIL"
    void send(String to, String title, String body);
}