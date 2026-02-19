package com.example.monitoring.worker.config;

import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import javax.net.ssl.SSLSocketFactory;
import java.util.Properties;

/**
 * SMTP 연결 시 TLS 1.0을 허용하는 메일 설정.
 * 메일 서버가 TLS 1.0만 지원할 때 SSL handshake 오류를 방지 (보안상 서버 측 TLS 1.2 이상 권장).
 */
@Configuration
public class MailConfig {

    @Value("${spring.mail.host:}")
    private String host;

    @Value("${spring.mail.port:587}")
    private int port;

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(host);
        sender.setPort(port);
        sender.setUsername(username);
        sender.setPassword(password);
        sender.setProtocol("smtp");

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.ssl.protocols", "TLSv1 TLSv1.1 TLSv1.2 TLSv1.3");
        SSLSocketFactory baseFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        props.put("mail.smtp.ssl.socketFactory", new Tls10AcceptingSSLSocketFactory(baseFactory));

        Authenticator authenticator = null;
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            authenticator = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            };
        }
        Session session = Session.getInstance(props, authenticator);
        sender.setSession(session);

        return sender;
    }
}
