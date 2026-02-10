package com.example.monitoring.worker.vpn;

import com.example.monitoring.common.domain.VpnConnectionEntity;
import com.example.monitoring.common.repo.VpnConnectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * VPN 상태 체크 스케줄러
 * 활성화된 VPN 연결을 주기적으로 체크
 */
@Component
public class VpnCheckScheduler {

    private static final Logger log = LoggerFactory.getLogger(VpnCheckScheduler.class);

    private final VpnConnectionRepository vpnRepo;
    private final VpnCheckService vpnCheckService;

    public VpnCheckScheduler(VpnConnectionRepository vpnRepo, VpnCheckService vpnCheckService) {
        this.vpnRepo = vpnRepo;
        this.vpnCheckService = vpnCheckService;
    }

    /**
     * VPN 상태 체크 (기본 60초마다 실행)
     * 각 VPN의 check_interval_sec에 따라 체크 여부 결정
     */
    @Scheduled(fixedDelayString = "${vpn.check.interval:60000}") // 기본 60초
    public void checkVpnStatuses() {
        try {
            List<VpnConnectionEntity> vpns = vpnRepo.findByEnabledTrue();
            
            for (VpnConnectionEntity vpn : vpns) {
                // 각 VPN의 check_interval_sec에 따라 체크 여부 결정
                if (shouldCheck(vpn)) {
                    vpnCheckService.checkVpnStatus(vpn);
                }
            }
        } catch (Exception e) {
            log.error("Error in VPN check scheduler", e);
        }
    }

    /**
     * VPN 체크 주기 확인
     */
    private boolean shouldCheck(VpnConnectionEntity vpn) {
        if (vpn.getLastCheckedAt() == null) {
            return true;
        }

        OffsetDateTime nextCheck = vpn.getLastCheckedAt()
                .plusSeconds(vpn.getCheckIntervalSec());
        
        return OffsetDateTime.now().isAfter(nextCheck);
    }
}
