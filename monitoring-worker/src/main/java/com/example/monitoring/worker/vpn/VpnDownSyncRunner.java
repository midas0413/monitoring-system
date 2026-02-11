package com.example.monitoring.worker.vpn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 워커 기동 시 DB에 이미 DOWN인 VPN에 대해 연결된 모니터링 룰 비활성화 동기화.
 * WorkerLoop보다 먼저 실행되어, 선점 시점에 DOWN VPN의 룰이 이미 비활성화되도록 함.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class VpnDownSyncRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(VpnDownSyncRunner.class);

    private final VpnCheckService vpnCheckService;

    public VpnDownSyncRunner(VpnCheckService vpnCheckService) {
        this.vpnCheckService = vpnCheckService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            vpnCheckService.syncRulesForDownVpns();
        } catch (Exception e) {
            log.error("Startup sync of monitoring rules for DOWN VPNs failed", e);
        }
    }
}
