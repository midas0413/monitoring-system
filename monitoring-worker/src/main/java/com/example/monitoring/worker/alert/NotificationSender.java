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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(NotificationSender.class);

    private final WorkerProperties props;
    private final NotificationOutboxRepository outboxRepo;
    private final Map<NotificationChannel, NotificationDeliverer> delivererMap;

    public NotificationSender(
            WorkerProperties props,
            NotificationOutboxRepository outboxRepo,
            List<NotificationDeliverer> deliverers
    ) {
        this.props = props;
        this.outboxRepo = outboxRepo;

        Map<NotificationChannel, NotificationDeliverer> map = new EnumMap<>(NotificationChannel.class);
        for (NotificationDeliverer d : deliverers) {
            map.put(d.channel(), d);
        }
        this.delivererMap = map;
    }

    /**
     * tick 주기는 WorkerProperties로 조절하는게 깔끔하지만,
     * 일단 fixedDelayString으로 걸어두면 운영에서 설정 변경이 쉬움.
     */
    @Scheduled(fixedDelayString = "${worker.outbox-tick-ms:1000}")
    public void tick() {
        OffsetDateTime now = OffsetDateTime.now();
        int limit = Math.max(1, props.getOutboxClaimLimit());

        List<NotificationOutboxEntity> claimed = outboxRepo.claimDue(now, limit);
        if (claimed.isEmpty()) return;

        for (NotificationOutboxEntity n : claimed) {
            try {
                processOne(n);
            } catch (Exception e) {
                log.error("Outbox send error. id={}, channel={}, to={}",
                        n.getId(), n.getChannel(), n.getToAddr(), e);
                failAndScheduleRetry(n, safeMsg(e));
            }
        }
    }

    @Transactional
    protected void processOne(NotificationOutboxEntity n) {
        NotificationChannel channel = n.getChannel();
        NotificationDeliverer deliverer = delivererMap.get(channel);

        if (deliverer == null) {
            failAndScheduleRetry(n, "No deliverer for channel=" + channel);
            return;
        }

        String to = n.getToAddr();
        String title = n.getTitle();
        String body = n.getBody();

        NotificationDeliverer.DeliverResult r = deliverer.deliver(to, title, body);

        if (r.success()) {
            n.setStatus(NotificationStatus.SENT);
            n.setSentAt(OffsetDateTime.now());
            n.setLastError(null);
            // nextAttemptAt은 의미 없어지지만 남겨둬도 무방
            outboxRepo.save(n);
        } else {
            failAndScheduleRetry(n, StringUtils.hasText(r.providerMessage()) ? r.providerMessage() : "deliver failed");
        }
    }

    private void failAndScheduleRetry(NotificationOutboxEntity n, String reason) {
        int attempt = (n.getAttempt() == null) ? 0 : n.getAttempt();
        int maxAttempt = (n.getMaxAttempt() == null) ? 5 : n.getMaxAttempt();

        attempt++;
        n.setAttempt(attempt);
        n.setLastError(trim(reason, 1000));

        if (attempt >= maxAttempt) {
            n.setStatus(NotificationStatus.FAILED);
            // 더 이상 재시도 없음
        } else {
            n.setStatus(NotificationStatus.PENDING);
            long backoffSec = computeBackoffSec(attempt);
            n.setNextAttemptAt(OffsetDateTime.now().plusSeconds(backoffSec));
        }

        outboxRepo.save(n);
    }

    private long computeBackoffSec(int attempt) {
        // 5s, 10s, 20s, 40s... (상한 5분)
        long base = 5L;
        long sec = base * (1L << Math.max(0, attempt - 1));
        return Math.min(sec, 300L);
    }

    private String safeMsg(Exception e) {
        String m = e.getMessage();
        if (StringUtils.hasText(m)) return m;
        return e.getClass().getSimpleName();
    }

    private String trim(String s, int max) {
        if (s == null) return null;
        return (s.length() <= max) ? s : s.substring(0, max);
    }
}