package com.example.monitoring.web.controller;

import com.example.monitoring.common.domain.CheckRunEntity;
import com.example.monitoring.common.domain.ServerEntity;
import com.example.monitoring.common.domain.ServerStatus;
import com.example.monitoring.common.domain.VpnConnectionEntity;
import com.example.monitoring.web.service.CheckRunService;
import com.example.monitoring.web.service.HomeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Home 화면 리프레시용 API
 * 알림 건수와 Check Runs 데이터를 JSON으로 반환
 */
@RestController
@RequestMapping("/api/home/refresh")
public class HomeRefreshApiController {

    private final HomeService homeService;
    private final CheckRunService checkRunService;

    public HomeRefreshApiController(HomeService homeService, CheckRunService checkRunService) {
        this.homeService = homeService;
        this.checkRunService = checkRunService;
    }

    @GetMapping
    public Map<String, Object> refresh(@RequestParam(required = false) Long ruleId) {
        // 서버 목록
        List<ServerEntity> servers = homeService.listAllServers();
        
        // 서버별 알림 건수
        Map<Long, Long> notificationCountByServer = homeService.countNotificationsByServer();
        
        // 서버 상태 맵 (서버 ID -> 상태)
        Map<Long, ServerStatus> serverStatusMap = new HashMap<>();
        for (ServerEntity server : servers) {
            serverStatusMap.put(server.getId(), homeService.getServerStatus(server));
        }

        // Check Runs (모니터링 결과)
        List<CheckRunEntity> checkRuns = checkRunService.list(ruleId, null);
        Map<Long, String> startedDisplay = checkRunService.buildStartedDisplayMap(checkRuns);
        Map<Long, String> ruleNameDisplay = checkRunService.buildRuleNameDisplayMap(checkRuns);
        Map<Long, String> serverDisplay = checkRunService.buildServerDisplayMap(checkRuns);
        
        // 서버 카드 배지용: 서버 ID(문자열) -> 상태(문자열) (JSON 직렬화 일관성)
        Map<String, String> serverStatusMapForJson = new HashMap<>();
        for (ServerEntity server : servers) {
            ServerStatus status = serverStatusMap.get(server.getId());
            serverStatusMapForJson.put(String.valueOf(server.getId()), status != null ? status.name() : "UNKNOWN");
        }
        // 서버명 기준 (기존 호환성)
        Map<String, String> serverStatusByTarget = new HashMap<>();
        for (ServerEntity server : servers) {
            ServerStatus status = serverStatusMap.get(server.getId());
            serverStatusByTarget.put(server.getName(), status != null ? status.name() : "UNKNOWN");
        }

        // VPN 상태 목록
        List<VpnConnectionEntity> vpns = homeService.listAllVpns();
        // VPN 상태 맵을 문자열로 변환 (JavaScript에서 사용하기 위해)
        Map<Long, String> vpnStatusMap = vpns.stream()
                .collect(Collectors.toMap(
                        VpnConnectionEntity::getId,
                        vpn -> vpn.getStatus() != null ? vpn.getStatus().name() : "UNKNOWN"
                ));

        Map<String, Object> result = new HashMap<>();
        result.put("notificationCountByServer", notificationCountByServer);
        result.put("serverStatusMap", serverStatusMapForJson);
        result.put("checkRuns", checkRuns);
        result.put("startedDisplay", startedDisplay);
        result.put("ruleNameDisplay", ruleNameDisplay);
        result.put("serverDisplay", serverDisplay);
        result.put("serverStatusByTarget", serverStatusByTarget);
        result.put("vpns", vpns);
        result.put("vpnStatusMap", vpnStatusMap);
        return result;
    }
}
