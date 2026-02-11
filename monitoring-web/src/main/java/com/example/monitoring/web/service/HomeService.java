package com.example.monitoring.web.service;

import com.example.monitoring.common.domain.*;
import com.example.monitoring.common.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 홈 대시보드용 데이터 조회 (Server 기반)
 */
@Service
@Transactional(readOnly = true)
public class HomeService {

    private final ServerRepository serverRepo;
    private final MonitoringRuleRepository ruleRepo;
    private final VpnConnectionRepository vpnRepo;
    private final NotificationOutboxRepository notificationOutboxRepository;

    public HomeService(ServerRepository serverRepo,
                       MonitoringRuleRepository ruleRepo,
                       VpnConnectionRepository vpnRepo,
                       NotificationOutboxRepository notificationOutboxRepository) {
        this.serverRepo = serverRepo;
        this.ruleRepo = ruleRepo;
        this.vpnRepo = vpnRepo;
        this.notificationOutboxRepository = notificationOutboxRepository;
    }

    /** 모든 활성화된 서버 목록 */
    public List<ServerEntity> listAllServers() {
        return serverRepo.findAll().stream()
                .filter(s -> Boolean.TRUE.equals(s.getEnabled()))
                .sorted((a, b) -> Long.compare(b.getId(), a.getId()))
                .collect(Collectors.toList());
    }

    /** 서버별 모니터링 룰 그룹화 */
    public Map<Long, List<MonitoringRuleEntity>> listRulesGroupByServer() {
        List<MonitoringRuleEntity> allRules = ruleRepo.findAll();
        return allRules.stream()
                .filter(r -> Boolean.TRUE.equals(r.getEnabled()))
                .collect(Collectors.groupingBy(MonitoringRuleEntity::getServerId));
    }

    /** 서버별 알림 건수 */
    public Map<Long, Long> countNotificationsByServer() {
        Map<Long, Long> result = new HashMap<>();
        List<ServerEntity> servers = listAllServers();
        for (ServerEntity server : servers) {
            List<MonitoringRuleEntity> rules = ruleRepo.findByServerIdAndEnabledTrue(server.getId());
            long totalCount = 0;
            for (MonitoringRuleEntity rule : rules) {
                totalCount += notificationOutboxRepository.countSentByMonitoringRuleId(rule.getId(), NotificationStatus.SENT);
            }
            result.put(server.getId(), totalCount);
        }
        return result;
    }

    /** 모든 활성화된 VPN 목록 */
    public List<VpnConnectionEntity> listAllVpns() {
        List<VpnConnectionEntity> vpns = vpnRepo.findByEnabledTrueOrderByNameAsc();
        // DB에서 조회한 시간을 한국 시간(KST, UTC+9)으로 변환
        ZoneId kstZone = ZoneId.of("Asia/Seoul");
        for (VpnConnectionEntity vpn : vpns) {
            if (vpn.getLastCheckedAt() != null) {
                // UTC로 저장된 시간을 한국 시간으로 변환
                vpn.setLastCheckedAt(vpn.getLastCheckedAt().atZoneSameInstant(kstZone).toOffsetDateTime());
            }
            if (vpn.getLastStatusChangeAt() != null) {
                vpn.setLastStatusChangeAt(vpn.getLastStatusChangeAt().atZoneSameInstant(kstZone).toOffsetDateTime());
            }
        }
        return vpns;
    }

    /** 서버 연결 상태 확인 (간단한 구현 - 실제로는 체크 결과 기반) */
    public ServerStatus getServerStatus(ServerEntity server) {
        // TODO: 실제 서버 연결 상태 체크 로직 구현
        // 현재는 UNKNOWN 반환 (워커가 동작하지 않아도 표시하기 위해)
        return ServerStatus.UNKNOWN;
    }

    public long countSentNotificationsByCheck(Long checkId) {
        return notificationOutboxRepository.countSentByCheckId(checkId, NotificationStatus.SENT);
    }
}
