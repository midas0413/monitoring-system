package com.example.monitoring.web.service;

import com.example.monitoring.common.domain.*;
import com.example.monitoring.common.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
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
    private final CheckRunRepository checkRunRepository;

    public HomeService(ServerRepository serverRepo,
                       MonitoringRuleRepository ruleRepo,
                       VpnConnectionRepository vpnRepo,
                       NotificationOutboxRepository notificationOutboxRepository,
                       CheckRunRepository checkRunRepository) {
        this.serverRepo = serverRepo;
        this.ruleRepo = ruleRepo;
        this.vpnRepo = vpnRepo;
        this.notificationOutboxRepository = notificationOutboxRepository;
        this.checkRunRepository = checkRunRepository;
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

    /** 룰별 알림 건수 */
    public Map<Long, Long> countNotificationsByRule() {
        Map<Long, Long> result = new HashMap<>();
        List<MonitoringRuleEntity> allRules = ruleRepo.findAll();
        for (MonitoringRuleEntity rule : allRules) {
            if (Boolean.TRUE.equals(rule.getEnabled())) {
                long count = notificationOutboxRepository.countSentByMonitoringRuleId(rule.getId(), NotificationStatus.SENT);
                result.put(rule.getId(), count);
            }
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

    /** 서버 연결 상태 확인 (체크 결과 기반) */
    public ServerStatus getServerStatus(ServerEntity server) {
        // 해당 서버의 최근 체크 실행 결과를 기반으로 상태 판단
        List<MonitoringRuleEntity> rules = ruleRepo.findByServerIdAndEnabledTrue(server.getId());
        if (rules.isEmpty()) {
            return ServerStatus.UNKNOWN;
        }
        
        // 최근 체크 실행 결과 조회 (각 룰별 최근 1개씩)
        boolean hasSuccess = false;
        boolean hasFailure = false;
        OffsetDateTime latestCheckTime = null;
        int checkedRulesCount = 0;
        
        for (MonitoringRuleEntity rule : rules) {
            // 최근 체크 실행 결과 조회
            List<CheckRunEntity> latestRuns = checkRunRepository
                    .findTop100ByMonitoringRuleIdOrderByStartedAtDesc(rule.getId());
            CheckRunEntity latestRun = latestRuns.isEmpty() ? null : latestRuns.get(0);
            
            if (latestRun != null) {
                checkedRulesCount++;
                if (latestCheckTime == null || latestRun.getStartedAt().isAfter(latestCheckTime)) {
                    latestCheckTime = latestRun.getStartedAt();
                }
                
                if (Boolean.TRUE.equals(latestRun.getSuccess())) {
                    hasSuccess = true;
                } else {
                    hasFailure = true;
                }
            }
        }
        
        // 최근 체크 결과가 없으면 UNKNOWN
        if (latestCheckTime == null) {
            return ServerStatus.UNKNOWN;
        }
        
        // 최근 체크가 10분 이내인지 확인 (너무 오래된 결과는 무시)
        // 체크 간격이 60초인 경우를 고려하여 10분으로 설정
        OffsetDateTime tenMinutesAgo = OffsetDateTime.now().minusMinutes(10);
        if (latestCheckTime.isBefore(tenMinutesAgo)) {
            return ServerStatus.UNKNOWN;
        }
        
        // 성공과 실패가 모두 있으면 실패 우선, 성공만 있으면 UP, 실패만 있으면 DOWN
        if (hasFailure) {
            return ServerStatus.DOWN;
        } else if (hasSuccess) {
            return ServerStatus.UP;
        } else {
            return ServerStatus.UNKNOWN;
        }
    }

    public long countSentNotificationsByCheck(Long checkId) {
        return notificationOutboxRepository.countSentByCheckId(checkId, NotificationStatus.SENT);
    }
}
