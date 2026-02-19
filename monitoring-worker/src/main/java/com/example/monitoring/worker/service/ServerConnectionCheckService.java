package com.example.monitoring.worker.service;

import com.example.monitoring.common.domain.ServerEntity;
import com.example.monitoring.common.domain.ServerStatus;
import com.example.monitoring.common.repo.ServerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 서버 연결상태를 설정된 주기(초)마다 ping으로 체크하여 갱신
 */
@Service
public class ServerConnectionCheckService {

    private static final Logger log = LoggerFactory.getLogger(ServerConnectionCheckService.class);
    private static final int PING_TIMEOUT_MS = 3000;

    private final ServerRepository serverRepo;
    private final ServerConnectionCheckService self;

    public ServerConnectionCheckService(ServerRepository serverRepo,
                                        @Lazy ServerConnectionCheckService self) {
        this.serverRepo = serverRepo;
        this.self = self;
    }

    /**
     * 30초마다 실행. 체크 주기가 설정된 서버 중 주기가 지난 경우 ping 후 연결상태 갱신
     * self.checkAndUpdate()로 호출해 별도 트랜잭션에서 저장되도록 함 (같은 클래스 내부 호출 시 프록시 미적용 방지)
     */
    @Scheduled(fixedDelay = 30000)
    public void runScheduledCheck() {
        List<ServerEntity> servers = serverRepo.findByEnabledTrueAndConnectionCheckIntervalSecGreaterThanOrderByNameAsc(0);
        if (servers.isEmpty()) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        for (ServerEntity server : servers) {
            try {
                if (!shouldCheckNow(server, now)) {
                    continue;
                }
                self.checkAndUpdate(server);
            } catch (Exception e) {
                log.warn("Server connection check failed. serverId={}, name={}, host={}", 
                        server.getId(), server.getName(), server.getHost(), e);
            }
        }
    }

    private boolean shouldCheckNow(ServerEntity server, OffsetDateTime now) {
        Integer intervalSec = server.getConnectionCheckIntervalSec();
        if (intervalSec == null || intervalSec <= 0) {
            return false;
        }
        OffsetDateTime last = server.getLastConnectionCheckAt();
        if (last == null) {
            return true;
        }
        return last.plusSeconds(intervalSec).isBefore(now) || last.plusSeconds(intervalSec).isEqual(now);
    }

    @Transactional
    public void checkAndUpdate(ServerEntity server) {
        ServerEntity entity = serverRepo.findById(server.getId()).orElse(null);
        if (entity == null) return;

        String host = extractPingHost(entity.getHost());
        if (!StringUtils.hasText(host)) {
            log.debug("Server host is empty. serverId={}", entity.getId());
            return;
        }

        boolean reachable = pingHost(host);
        ServerStatus newStatus = reachable ? ServerStatus.UP : ServerStatus.DOWN;
        OffsetDateTime now = OffsetDateTime.now();

        entity.setConnectionStatus(newStatus);
        entity.setLastConnectionCheckAt(now);
        serverRepo.saveAndFlush(entity);

        log.info("Server connection check: serverId={}, name={}, host={}, status={}", 
                entity.getId(), entity.getName(), host, newStatus);
    }

    /** host 문자열에서 ping 대상 호스트만 추출 (예: "192.168.1.1:22" -> "192.168.1.1") */
    private String extractPingHost(String host) {
        if (host == null) return null;
        int colon = host.indexOf(':');
        return colon > 0 ? host.substring(0, colon).trim() : host.trim();
    }

    private boolean pingHost(String host) {
        try {
            InetAddress address = InetAddress.getByName(host);
            return address.isReachable(PING_TIMEOUT_MS);
        } catch (Exception e) {
            log.trace("Ping failed for host={}: {}", host, e.getMessage());
            return false;
        }
    }
}
