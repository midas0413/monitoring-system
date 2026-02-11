package com.example.monitoring.worker.vpn;

import com.example.monitoring.common.domain.ServerEntity;
import com.example.monitoring.common.domain.ServerStatus;
import com.example.monitoring.common.domain.ServerVpnLinkEntity;
import com.example.monitoring.common.domain.VpnConnectionEntity;
import com.example.monitoring.common.repo.MonitoringRuleRepository;
import com.example.monitoring.common.repo.ServerRepository;
import com.example.monitoring.common.repo.ServerVpnLinkRepository;
import com.example.monitoring.common.repo.VpnConnectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

@Service
@Transactional
public class VpnCheckService {

    private static final Logger log = LoggerFactory.getLogger(VpnCheckService.class);
    private static final int CONNECTION_TIMEOUT_MS = 5000;

    private final VpnConnectionRepository vpnRepo;
    private final ServerVpnLinkRepository linkRepo;
    private final ServerRepository serverRepo;
    private final MonitoringRuleRepository monitoringRuleRepo;
    private final VpnStatusChangeNotifier notifier;

    public VpnCheckService(VpnConnectionRepository vpnRepo,
                          ServerVpnLinkRepository linkRepo,
                          ServerRepository serverRepo,
                          MonitoringRuleRepository monitoringRuleRepo,
                          VpnStatusChangeNotifier notifier) {
        this.vpnRepo = vpnRepo;
        this.linkRepo = linkRepo;
        this.serverRepo = serverRepo;
        this.monitoringRuleRepo = monitoringRuleRepo;
        this.notifier = notifier;
    }

    /**
     * VPN 연결 상태 체크
     */
    @Transactional
    public void checkVpnStatus(VpnConnectionEntity vpn) {
        log.info("Checking VPN status: id={}, name={}, host={}, currentStatus={}", 
                vpn.getId(), vpn.getName(), vpn.getHost(), vpn.getStatus());

        ServerStatus newStatus = checkConnectivity(vpn.getHost());
        ServerStatus oldStatus = vpn.getStatus();

        // 한국 시간(KST, UTC+9)으로 저장
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.of("+09:00"));
        
        log.info("VPN connectivity check result: id={}, name={}, host={}, oldStatus={}, newStatus={}", 
                vpn.getId(), vpn.getName(), vpn.getHost(), oldStatus, newStatus);

        // 항상 lastCheckedAt 업데이트
        vpn.setLastCheckedAt(now);

        // 상태 변경 감지 (null인 경우도 변경으로 간주)
        if (oldStatus != newStatus || oldStatus == null) {
            log.info("VPN status changed: id={}, name={}, {} -> {}", 
                    vpn.getId(), vpn.getName(), oldStatus, newStatus);
            
            vpn.setStatus(newStatus);
            if (oldStatus != null && oldStatus != newStatus) {
                vpn.setLastStatusChangeAt(now);
            }
            
            // 명시적으로 저장 및 플러시
            vpnRepo.saveAndFlush(vpn);
            log.info("VPN status saved: id={}, name={}, status={}, lastCheckedAt={}, lastStatusChangeAt={}", 
                    vpn.getId(), vpn.getName(), vpn.getStatus(), vpn.getLastCheckedAt(), vpn.getLastStatusChangeAt());

            // 상태 변경 알림 (null이 아닌 경우에만)
            if (oldStatus != null) {
                notifier.notifyStatusChange(vpn, oldStatus, newStatus);
            }

            // VPN Down 시 해당 서버의 모니터링 룰 비활성화
            if (newStatus == ServerStatus.DOWN) {
                disableMonitoringForVpn(vpn.getId());
            } 
            // VPN Up 시 모니터링 룰 재활성화
            else if (newStatus == ServerStatus.UP && oldStatus == ServerStatus.DOWN) {
                enableMonitoringForVpn(vpn.getId());
            }
        } else {
            // 상태가 변경되지 않아도 lastCheckedAt 업데이트를 위해 저장
            vpn.setStatus(newStatus); // 현재 상태 명시적으로 설정
            vpnRepo.saveAndFlush(vpn);
            log.debug("VPN status unchanged but updated lastCheckedAt: id={}, name={}, status={}, lastCheckedAt={}", 
                    vpn.getId(), vpn.getName(), vpn.getStatus(), vpn.getLastCheckedAt());
        }
    }

    /**
     * 모든 활성화된 VPN 연결 상태 체크
     * 10초마다 실행되며, 각 VPN의 checkIntervalSec 설정에 따라 체크 여부 결정
     */
    @Scheduled(fixedDelay = 10000) // 10초마다 실행 (각 VPN의 설정된 주기 확인)
    @Transactional
    public void checkAllVpns() {
        List<VpnConnectionEntity> vpns = vpnRepo.findByEnabledTrue();
        if (vpns.isEmpty()) {
            log.trace("No enabled VPNs to check");
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        int checkedCount = 0;
        int skippedCount = 0;

        log.debug("Starting VPN check cycle. Total enabled VPNs: {}", vpns.size());

        for (VpnConnectionEntity vpn : vpns) {
            try {
                // DB에서 최신 정보 다시 조회 (detached 상태 방지)
                VpnConnectionEntity freshVpn = vpnRepo.findById(vpn.getId()).orElse(null);
                if (freshVpn == null || !Boolean.TRUE.equals(freshVpn.getEnabled())) {
                    continue;
                }

                // 각 VPN의 체크 주기 확인
                Integer intervalSec = freshVpn.getCheckIntervalSec() != null ? freshVpn.getCheckIntervalSec() : 60;
                
                // 마지막 체크 시간 확인
                if (freshVpn.getLastCheckedAt() != null) {
                    OffsetDateTime nextCheckTime = freshVpn.getLastCheckedAt().plusSeconds(intervalSec);
                    if (now.isBefore(nextCheckTime)) {
                        // 아직 체크 주기가 지나지 않음
                        skippedCount++;
                        log.trace("Skipping VPN check: id={}, name={}, nextCheck={}", 
                                freshVpn.getId(), freshVpn.getName(), nextCheckTime);
                        continue;
                    }
                }

                // 체크 주기가 지났거나 처음 체크하는 경우
                log.info("Checking VPN: id={}, name={}, host={}, lastChecked={}", 
                        freshVpn.getId(), freshVpn.getName(), freshVpn.getHost(), freshVpn.getLastCheckedAt());
                checkVpnStatus(freshVpn);
                checkedCount++;
            } catch (Exception e) {
                log.error("Error checking VPN: id={}, name={}", vpn.getId(), vpn.getName(), e);
            }
        }

        if (checkedCount > 0 || skippedCount > 0) {
            log.info("VPN check cycle completed: {} checked, {} skipped (total: {})", 
                    checkedCount, skippedCount, vpns.size());
        }
    }

    /**
     * VPN 호스트 연결성 체크
     * TCP 연결 테스트만 사용 (Ping은 신뢰할 수 없음)
     */
    private ServerStatus checkConnectivity(String host) {
        if (host == null || host.isBlank()) {
            log.warn("VPN host is null or blank");
            return ServerStatus.UNKNOWN;
        }

        try {
            // 호스트에서 포트 추출 (host:port 형식 지원)
            String[] parts = host.split(":");
            String hostname = parts[0].trim();
            int specifiedPort = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : -1;

            if (hostname.isEmpty()) {
                log.warn("VPN hostname is empty: host={}", host);
                return ServerStatus.UNKNOWN;
            }

            log.debug("Checking VPN connectivity: hostname={}, specifiedPort={}", hostname, specifiedPort);

            // TCP 연결 테스트만 사용 (Ping은 방화벽에서 차단될 수 있어 신뢰할 수 없음)
            int[] portsToTest;
            if (specifiedPort > 0) {
                portsToTest = new int[]{specifiedPort};
            } else {
                // 기본 포트들 시도: 443, 80, 22, 8080 (HTTPS 우선)
                portsToTest = new int[]{443, 80, 22, 8080};
            }

            boolean anyPortReachable = false;
            for (int port : portsToTest) {
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(hostname, port), CONNECTION_TIMEOUT_MS);
                    log.info("VPN TCP connection success: hostname={}, port={}", hostname, port);
                    return ServerStatus.UP;
                } catch (java.net.ConnectException e) {
                    // 연결 거부 - 포트는 열려있지만 서비스가 없거나 거부
                    log.debug("VPN TCP connection refused: hostname={}, port={}", hostname, port);
                    anyPortReachable = true; // 포트는 열려있음
                } catch (java.net.SocketTimeoutException e) {
                    // 타임아웃 - 연결 불가
                    log.debug("VPN TCP connection timeout: hostname={}, port={}", hostname, port);
                } catch (java.net.UnknownHostException e) {
                    // 호스트를 찾을 수 없음
                    log.warn("VPN host not found: hostname={}, error={}", hostname, e.getMessage());
                    return ServerStatus.DOWN;
                } catch (Exception e) {
                    // 기타 오류
                    log.debug("VPN TCP connection failed: hostname={}, port={}, error={}", hostname, port, e.getMessage());
                }
            }

            // 모든 포트 테스트 실패
            if (anyPortReachable) {
                // 일부 포트는 열려있지만 연결 실패
                log.info("VPN connection failed: hostname={}, ports tested but connection failed", hostname);
            } else {
                // 모든 포트 연결 불가
                log.info("VPN connection failed: hostname={}, all ports unreachable", hostname);
            }
            return ServerStatus.DOWN;

        } catch (NumberFormatException e) {
            log.warn("Invalid port in host: {}, error={}", host, e.getMessage());
            return ServerStatus.UNKNOWN;
        } catch (Exception e) {
            log.error("VPN connection check error: host={}, error={}", host, e.getMessage(), e);
            return ServerStatus.DOWN;
        }
    }

    /**
     * VPN Down 시 해당 VPN을 사용하는 서버의 모니터링 룰 비활성화
     */
    private void disableMonitoringForVpn(Long vpnId) {
        List<ServerVpnLinkEntity> links = linkRepo.findByVpnIdAndEnabledTrue(vpnId);
        log.info("Disabling monitoring for {} servers using VPN: vpnId={}", links.size(), vpnId);

        for (ServerVpnLinkEntity link : links) {
            Long serverId = link.getServerId();
            int disabledCount = monitoringRuleRepo.findByServerIdAndEnabledTrue(serverId).stream()
                    .mapToInt(rule -> {
                        rule.setEnabled(false);
                        monitoringRuleRepo.save(rule);
                        return 1;
                    })
                    .sum();
            
            if (disabledCount > 0) {
                log.info("Disabled {} monitoring rules for server: serverId={}", disabledCount, serverId);
            }
        }
    }

    /**
     * VPN Up 시 해당 VPN을 사용하는 서버의 모니터링 룰 재활성화
     */
    private void enableMonitoringForVpn(Long vpnId) {
        List<ServerVpnLinkEntity> links = linkRepo.findByVpnIdAndEnabledTrue(vpnId);
        log.info("Enabling monitoring for {} servers using VPN: vpnId={}", links.size(), vpnId);

        for (ServerVpnLinkEntity link : links) {
            Long serverId = link.getServerId();
            // 서버가 활성화되어 있는 경우에만 모니터링 룰 재활성화
            ServerEntity server = serverRepo.findById(serverId).orElse(null);
            if (server != null && server.getEnabled()) {
                int enabledCount = monitoringRuleRepo.findByServerId(serverId).stream()
                        .mapToInt(rule -> {
                            rule.setEnabled(true);
                            monitoringRuleRepo.save(rule);
                            return 1;
                        })
                        .sum();
                
                if (enabledCount > 0) {
                    log.info("Enabled {} monitoring rules for server: serverId={}", enabledCount, serverId);
                }
            }
        }
    }
}
