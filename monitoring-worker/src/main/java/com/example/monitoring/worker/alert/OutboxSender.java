package com.example.monitoring.worker.alert;

import com.example.monitoring.common.domain.NotificationChannel;
import com.example.monitoring.common.domain.NotificationOutboxEntity;
import com.example.monitoring.common.domain.NotificationStatus;
import com.example.monitoring.common.repo.NotificationOutboxRepository;
import com.example.monitoring.worker.WorkerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;

@Component
public class OutboxSender {

    private static final Logger log = LoggerFactory.getLogger(OutboxSender.class);

    private final WorkerProperties props;
    private final NotificationOutboxRepository outboxRepository;
    private final DelivererRouter delivererRouter;

    public OutboxSender(
            WorkerProperties props,
            NotificationOutboxRepository outboxRepository,
            DelivererRouter delivererRouter
    ) {
        this.props = props;
        this.outboxRepository = outboxRepository;
        this.delivererRouter = delivererRouter;
    }

    @Scheduled(fixedDelayString = "${worker.outbox-tick-ms:1000}")
    public void tick() {
        OffsetDateTime now = OffsetDateTime.now();
        int limit = Math.max(1, props.getOutboxClaimLimit());

        // ✅ 멀티워커 안전 선점 (SKIP LOCKED + status=PROCESSING)
        List<NotificationOutboxEntity> claimed = outboxRepository.claimDue(now, limit);
        if (claimed.isEmpty()) return;

        for (NotificationOutboxEntity n : claimed) {
            try {
                processOne(n);
            } catch (Exception e) {
                log.error("Outbox send error. id={}, channel={}, to={}",
                        n.getId(), n.getChannel(), n.getToAddr(), e);
                failAndRetry(n, safeMsg(e));
            }
        }
    }

    @Transactional
    protected void processOne(NotificationOutboxEntity n) {
        var deliverer = delivererRouter.route(n.getChannel());

        String to = n.getToAddr();
        String title = n.getTitle();
        String body = n.getBody();

        log.info("알림 발송 시도. id={}, channel={}, to={}, attempt={}/{}", 
                n.getId(), n.getChannel(), to, n.getAttempt(), n.getMaxAttempt());

        NotificationDeliverer.DeliverResult r;
        
        // KAKAO 채널일 때는 checkRunId와 템플릿 코드를 전달하여 정확한 서버명 추출 및 템플릿 코드 사용
        if (n.getChannel() == com.example.monitoring.common.domain.NotificationChannel.KAKAO 
                && deliverer instanceof KakaoDeliverer) {
            String templateCode = n.getKakaoTemplateCode();
            log.info("카카오 알림 발송 시도. outboxId={}, templateCode={}, checkRunId={}", 
                    n.getId(), templateCode, n.getCheckRunId());
            r = ((KakaoDeliverer) deliverer).deliverWithContext(to, title, body, n.getCheckRunId(), templateCode);
        } else {
            r = deliverer.deliver(to, title, body);
        }

        if (r.success()) {
            n.setStatus(NotificationStatus.SENT);
            n.setSentAt(OffsetDateTime.now());
            n.setLastError(null);
            outboxRepository.save(n);
            log.info("알림 발송 성공. id={}, channel={}, to={}, providerMessage={}", 
                    n.getId(), n.getChannel(), to, r.providerMessage());
        } else {
            log.warn("알림 발송 실패. id={}, channel={}, to={}, error={}", 
                    n.getId(), n.getChannel(), to, r.providerMessage());
            failAndRetry(n, StringUtils.hasText(r.providerMessage()) ? r.providerMessage() : "deliver failed");
        }
    }

    private void failAndRetry(NotificationOutboxEntity n, String reason) {
        int attempt = (n.getAttempt() == null) ? 0 : n.getAttempt();
        int maxAttempt = (n.getMaxAttempt() == null) ? 5 : n.getMaxAttempt();

        attempt++;
        n.setAttempt(attempt);
        n.setLastError(trim(reason, 1000));

        if (attempt >= maxAttempt) {
            n.setStatus(NotificationStatus.FAILED);
        } else {
            n.setStatus(NotificationStatus.PENDING);
            long backoffSec = computeBackoffSec(attempt);
            n.setNextAttemptAt(OffsetDateTime.now().plusSeconds(backoffSec));
        }

        outboxRepository.save(n);
    }

    private long computeBackoffSec(int attempt) {
        long base = 5L;
        long sec = base * (1L << Math.max(0, attempt - 1));
        return Math.min(sec, 300L);
    }

    private String safeMsg(Exception e) {
        String m = e.getMessage();
        return StringUtils.hasText(m) ? m : e.getClass().getSimpleName();
    }

    private String trim(String s, int max) {
        if (s == null) return null;
        return (s.length() <= max) ? s : s.substring(0, max);
    }
}