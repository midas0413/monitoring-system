package com.example.monitoring.worker.alert;

import com.example.monitoring.common.domain.NotificationChannel;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;

/**
 * EMAIL 알림 발송 - Gmail SMTP 연동
 * spring.mail 설정 필요 (username, password는 Gmail 앱 비밀번호)
 */
@Component
public class EmailDeliverer implements NotificationDeliverer {

    private static final Logger log = LoggerFactory.getLogger(EmailDeliverer.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailDeliverer(JavaMailSender mailSender,
                          @Value("${spring.mail.username:}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = StringUtils.hasText(fromAddress) ? fromAddress : "noreply@monitoring";
    }

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public DeliverResult deliver(String toAddr, String title, String body) {
        if (!StringUtils.hasText(toAddr)) {
            return DeliverResult.fail("수신자 정보 없음");
        }

        if (!toAddr.trim().contains("@")) {
            log.warn("[EMAIL] 수신자가 이메일 형식이 아님. toAddr={}", toAddr);
            return DeliverResult.fail("이메일 형식이 올바르지 않습니다.");
        }

        if ("noreply@monitoring".equals(fromAddress)) {
            log.warn("[EMAIL] Gmail 설정이 없습니다. spring.mail.username, spring.mail.password 를 설정하세요.");
            return DeliverResult.fail("메일 설정 부족 (spring.mail.username, spring.mail.password)");
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromAddress);
            helper.setTo(toAddr.trim());
            helper.setSubject(StringUtils.hasText(title) ? title : "[모니터링알림]");
            helper.setText(body != null ? body : "", false);  // plain text

            mailSender.send(message);
            log.info("[EMAIL] 발송 완료. to={}", toAddr);
            return DeliverResult.ok("sent");

        } catch (MessagingException e) {
            log.error("[EMAIL] 발송 실패. to={}", toAddr, e);
            return DeliverResult.fail(e.getMessage());
        }
    }
}