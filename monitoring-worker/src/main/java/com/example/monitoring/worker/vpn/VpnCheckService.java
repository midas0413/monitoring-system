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

import com.example.monitoring.common.domain.VpnCheckMethod;
import java.net.InetAddress;
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

        // DB에서 최신 상태 다시 조회 (detached entity 방지 및 동시성 문제 해결)
        VpnConnectionEntity freshVpn = vpnRepo.findById(vpn.getId()).orElse(null);
        if (freshVpn == null) {
            log.warn("VPN not found: id={}", vpn.getId());
            return;
        }

        ServerStatus newStatus = checkConnectivity(freshVpn.getHost(), freshVpn.getCheckMethod());
        ServerStatus oldStatus = freshVpn.getStatus();

        // 한국 시간(KST, UTC+9)으로 저장
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.of("+09:00"));
        
        log.info("VPN connectivity check result: id={}, name={}, host={}, oldStatus={}, newStatus={}", 
                freshVpn.getId(), freshVpn.getName(), freshVpn.getHost(), oldStatus, newStatus);
        
        // UP으로 판정된 경우 경고 메시지 추가
        if (newStatus == ServerStatus.UP) {
            log.warn("VPN status is UP for id={}, name={}, host={} - Please verify that VPN service is actually running, not just a web/SSH server", 
                    freshVpn.getId(), freshVpn.getName(), freshVpn.getHost());
        }

        // 항상 lastCheckedAt 업데이트
        freshVpn.setLastCheckedAt(now);

        // 상태 변경 감지 (null인 경우도 변경으로 간주)
        if (oldStatus != newStatus || oldStatus == null) {
            log.info("VPN status changed: id={}, name={}, {} -> {}", 
                    freshVpn.getId(), freshVpn.getName(), oldStatus, newStatus);
            
            freshVpn.setStatus(newStatus);
            if (oldStatus != null && oldStatus != newStatus) {
                freshVpn.setLastStatusChangeAt(now);
            }
            
            // 명시적으로 저장 및 플러시 (다른 트랜잭션에서 중복 체크 방지)
            vpnRepo.saveAndFlush(freshVpn);
            log.info("VPN status saved: id={}, name={}, status={}, lastCheckedAt={}, lastStatusChangeAt={}", 
                    freshVpn.getId(), freshVpn.getName(), freshVpn.getStatus(), freshVpn.getLastCheckedAt(), freshVpn.getLastStatusChangeAt());

            // 상태 변경 알림 (oldStatus가 null이 아니고 UNKNOWN이 아니며 실제로 변경된 경우에만)
            // 초기 상태(UNKNOWN)에서 변경되는 경우는 알림 발송하지 않음
            if (oldStatus != null && oldStatus != ServerStatus.UNKNOWN && oldStatus != newStatus) {
                // 최신 엔티티를 사용하여 알림 발송
                log.info("Sending VPN status change notification: vpn={}, {} -> {}", 
                        freshVpn.getName(), oldStatus, newStatus);
                notifier.notifyStatusChange(freshVpn, oldStatus, newStatus);
            } else if (oldStatus == null || oldStatus == ServerStatus.UNKNOWN) {
                log.info("Skipping notification for initial/unknown status: vpn={}, oldStatus={}, newStatus={}", 
                        freshVpn.getName(), oldStatus, newStatus);
            }

            // VPN Down 시 해당 서버의 모니터링 룰 비활성화 및 서버 연결상태 DOWN
            if (newStatus == ServerStatus.DOWN) {
                disableMonitoringForVpn(freshVpn.getId());
                setServersConnectionStatusDownForVpn(freshVpn.getId());
            } 
            // VPN Up 시 모니터링 룰 재활성화
            else if (newStatus == ServerStatus.UP && oldStatus == ServerStatus.DOWN) {
                enableMonitoringForVpn(freshVpn.getId());
            }
        } else {
            // 상태가 변경되지 않아도 lastCheckedAt 업데이트를 위해 저장
            freshVpn.setStatus(newStatus); // 현재 상태 명시적으로 설정
            vpnRepo.saveAndFlush(freshVpn);
            log.debug("VPN status unchanged but updated lastCheckedAt: id={}, name={}, status={}, lastCheckedAt={}", 
                    freshVpn.getId(), freshVpn.getName(), freshVpn.getStatus(), freshVpn.getLastCheckedAt());
            // VPN이 계속 DOWN인 경우에도 매 체크마다 룰 비활성화 및 서버 연결상태 DOWN 유지
            if (newStatus == ServerStatus.DOWN) {
                disableMonitoringForVpn(freshVpn.getId());
                setServersConnectionStatusDownForVpn(freshVpn.getId());
            }
        }
    }

    /**
     * VPN에 연결된 모든 서버의 연결상태를 DOWN으로 설정 (VPN 다운 시 호출)
     */
    private void setServersConnectionStatusDownForVpn(Long vpnId) {
        List<ServerVpnLinkEntity> links = linkRepo.findByVpnIdAndEnabledTrue(vpnId);
        for (ServerVpnLinkEntity link : links) {
            ServerEntity server = serverRepo.findById(link.getServerId()).orElse(null);
            if (server != null) {
                server.setConnectionStatus(ServerStatus.DOWN);
                serverRepo.save(server);
                log.info("Server connection status set to DOWN (VPN down): serverId={}, serverName={}, vpnId={}",
                        server.getId(), server.getName(), vpnId);
            }
        }
    }

    /**
     * DB에 이미 DOWN으로 저장된 VPN에 대해 연결된 모니터링 룰 비활성화 동기화.
     * 워커 기동 시 호출하여, 재시작 전에 DOWN이었던 VPN의 룰이 enabled로 남아 실행되는 것을 방지.
     */
    @Transactional
    public void syncRulesForDownVpns() {
        List<VpnConnectionEntity> downVpns = vpnRepo.findByEnabledTrueAndStatus(ServerStatus.DOWN);
        if (downVpns.isEmpty()) {
            log.debug("No enabled VPNs with status DOWN to sync");
            return;
        }
        log.info("Syncing monitoring rules for {} VPN(s) with status DOWN (startup)", downVpns.size());
        for (VpnConnectionEntity vpn : downVpns) {
            disableMonitoringForVpn(vpn.getId());
            setServersConnectionStatusDownForVpn(vpn.getId());
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
     * checkMethod에 따라 TCP, PING, 또는 둘 다 체크
     */
    private ServerStatus checkConnectivity(String host, VpnCheckMethod checkMethod) {
        if (checkMethod == null) {
            checkMethod = VpnCheckMethod.TCP; // 기본값
        }
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

            log.info("Checking VPN connectivity: hostname={}, specifiedPort={}, checkMethod={}", hostname, specifiedPort, checkMethod);

            // PING만 체크하는 경우
            if (checkMethod == VpnCheckMethod.PING) {
                return checkPing(hostname);
            }

            // TCP 체크
            boolean tcpSuccess = false;
            int[] portsToTest;
            if (specifiedPort > 0) {
                portsToTest = new int[]{specifiedPort};
                log.info("VPN will test specified port only: {}", specifiedPort);
            } else {
                // 기본 포트들 시도: 443, 80, 22, 8080 (HTTPS 우선)
                portsToTest = new int[]{443, 80, 22, 8080};
                log.info("VPN will test default ports in order: 443, 80, 22, 8080");
            }

            boolean anyPortReachable = false;
            int successfulPort = -1;
            for (int port : portsToTest) {
                try (Socket socket = new Socket()) {
                    log.info("Attempting TCP connection to {}:{} (timeout: {}ms)", hostname, port, CONNECTION_TIMEOUT_MS);
                    socket.connect(new InetSocketAddress(hostname, port), CONNECTION_TIMEOUT_MS);
                    successfulPort = port;
                    log.warn("VPN TCP connection success: hostname={}, port={} - WARNING: This port may not be a VPN service! (Could be web server, SSH, etc.)", hostname, port);
                    log.warn("VPN status set to UP based on port {} connection, but this does not guarantee VPN service is running", port);
                    log.warn("If VPN service is not actually running, please specify VPN-specific port in host field (e.g., {}:{}) or change checkMethod to PING", hostname, port);
                    tcpSuccess = true;
                    break; // 하나라도 성공하면 중단
                } catch (java.net.ConnectException e) {
                    // 연결 거부 - 포트는 열려있지만 서비스가 없거나 거부
                    log.info("VPN TCP connection refused: hostname={}, port={} (port is open but connection refused)", hostname, port);
                    anyPortReachable = true; // 포트는 열려있음
                } catch (java.net.SocketTimeoutException e) {
                    // 타임아웃 - 연결 불가
                    log.info("VPN TCP connection timeout: hostname={}, port={} (timeout after {}ms)", hostname, port, CONNECTION_TIMEOUT_MS);
                } catch (java.net.UnknownHostException e) {
                    // 호스트를 찾을 수 없음
                    log.warn("VPN host not found: hostname={}, error={}", hostname, e.getMessage());
                    // TCP만 체크하는 경우 즉시 DOWN 반환
                    if (checkMethod == VpnCheckMethod.TCP) {
                        return ServerStatus.DOWN;
                    }
                } catch (Exception e) {
                    // 기타 오류
                    log.info("VPN TCP connection failed: hostname={}, port={}, error={}", hostname, port, e.getMessage());
                }
            }

            // TCP만 체크하는 경우
            if (checkMethod == VpnCheckMethod.TCP) {
                if (tcpSuccess) {
                    return ServerStatus.UP;
                } else {
                    if (anyPortReachable) {
                        log.warn("VPN connection failed: hostname={}, some ports were reachable but all connections failed (ports tested: {})", 
                                hostname, java.util.Arrays.toString(portsToTest));
                    } else {
                        log.warn("VPN connection failed: hostname={}, all ports unreachable (ports tested: {})", 
                                hostname, java.util.Arrays.toString(portsToTest));
                    }
                    log.info("VPN status set to DOWN: hostname={}, all tested ports failed (ports tested: {})", 
                            hostname, java.util.Arrays.toString(portsToTest));
                    return ServerStatus.DOWN;
                }
            }

            // BOTH인 경우 PING도 체크
            if (checkMethod == VpnCheckMethod.BOTH) {
                boolean pingSuccess = checkPing(hostname) == ServerStatus.UP;
                if (tcpSuccess || pingSuccess) {
                    log.info("VPN status set to UP: hostname={}, TCP={}, PING={}", hostname, tcpSuccess, pingSuccess);
                    return ServerStatus.UP;
                } else {
                    log.warn("VPN connection failed: hostname={}, both TCP and PING failed", hostname);
                    return ServerStatus.DOWN;
                }
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
     * PING(ICMP) 체크
     */
    private ServerStatus checkPing(String hostname) {
        try {
            log.info("Attempting PING to {} (timeout: {}ms)", hostname, CONNECTION_TIMEOUT_MS);
            InetAddress address = InetAddress.getByName(hostname);
            boolean reachable = address.isReachable(CONNECTION_TIMEOUT_MS);
            
            if (reachable) {
                log.info("VPN PING success: hostname={}", hostname);
                return ServerStatus.UP;
            } else {
                log.info("VPN PING failed: hostname={} (host not reachable)", hostname);
                return ServerStatus.DOWN;
            }
        } catch (java.net.UnknownHostException e) {
            log.warn("VPN host not found (PING): hostname={}, error={}", hostname, e.getMessage());
            return ServerStatus.DOWN;
        } catch (Exception e) {
            log.warn("VPN PING check error: hostname={}, error={} - Note: PING may be blocked by firewall or require root/admin privileges", 
                    hostname, e.getMessage());
            // PING 실패는 방화벽이나 권한 문제일 수 있으므로 DOWN 반환
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
