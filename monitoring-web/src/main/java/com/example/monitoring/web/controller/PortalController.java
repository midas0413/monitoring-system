package com.example.monitoring.web.controller;

import com.example.monitoring.common.domain.*;
import com.example.monitoring.web.service.CheckRunService;
import com.example.monitoring.web.service.HomeService;
import com.example.monitoring.web.service.MonitoringRuleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class PortalController {

    private final HomeService homeService;
    private final CheckRunService checkRunService;
    private final MonitoringRuleService monitoringRuleService;

    public PortalController(HomeService homeService, 
                           CheckRunService checkRunService,
                           MonitoringRuleService monitoringRuleService) {
        this.homeService = homeService;
        this.checkRunService = checkRunService;
        this.monitoringRuleService = monitoringRuleService;
    }

    @GetMapping("/")
    public String home(
            @RequestParam(required = false) Long ruleId,
            Model model) {
        model.addAttribute("pageTitle", "Home");
        model.addAttribute("activeMenu", "home");
        model.addAttribute("content", "home :: content");

        // VPN 목록
        List<VpnConnectionEntity> vpns = homeService.listAllVpns();
        model.addAttribute("vpns", vpns);

        // 서버 목록
        List<ServerEntity> servers = homeService.listAllServers();
        model.addAttribute("servers", servers);

        // 서버별 룰 그룹화
        Map<Long, List<MonitoringRuleEntity>> rulesByServer = homeService.listRulesGroupByServer();
        model.addAttribute("rulesByServer", rulesByServer);

        // 서버별 알림 건수
        Map<Long, Long> notificationCountByServer = homeService.countNotificationsByServer();
        model.addAttribute("notificationCountByServer", notificationCountByServer);

        // 서버 상태 맵 (서버 ID -> 상태)
        Map<Long, ServerStatus> serverStatusMap = new HashMap<>();
        for (ServerEntity server : servers) {
            serverStatusMap.put(server.getId(), homeService.getServerStatus(server));
        }
        model.addAttribute("serverStatusMap", serverStatusMap);

        // Check Runs (모니터링 결과)
        List<CheckRunEntity> checkRuns = checkRunService.list(ruleId, null);
        model.addAttribute("checkRuns", checkRuns);
        model.addAttribute("startedDisplay", checkRunService.buildStartedDisplayMap(checkRuns));
        model.addAttribute("ruleNameDisplay", checkRunService.buildRuleNameDisplayMap(checkRuns));
        model.addAttribute("serverDisplay", checkRunService.buildServerDisplayMap(checkRuns));
        model.addAttribute("filterRuleId", ruleId);

        // 모든 룰 목록 (필터용)
        List<MonitoringRuleEntity> allRules = monitoringRuleService.list(null);
        model.addAttribute("allRules", allRules);

        return "layout";
    }
}
