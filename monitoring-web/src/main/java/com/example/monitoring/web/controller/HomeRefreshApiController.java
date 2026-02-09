package com.example.monitoring.web.controller;

import com.example.monitoring.common.domain.CheckEntity;
import com.example.monitoring.common.domain.CheckRunEntity;
import com.example.monitoring.web.service.CheckRunService;
import com.example.monitoring.web.service.HomeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public Map<String, Object> refresh(@RequestParam(required = false) Long checkId) {
        Map<String, List<CheckEntity>> checksByTarget = homeService.listChecksGroupByTarget();

        Map<Long, Long> notificationCountByCheck = new HashMap<>();
        for (List<CheckEntity> checks : checksByTarget.values()) {
            for (CheckEntity c : checks) {
                notificationCountByCheck.put(c.getId(), homeService.countSentNotificationsByCheck(c.getId()));
            }
        }

        List<CheckRunEntity> checkRuns = checkRunService.list(checkId, null);
        Map<Long, String> startedDisplay = checkRunService.buildStartedDisplayMap(checkRuns);
        Map<Long, String> ruleNameDisplay = checkRunService.buildRuleNameDisplayMap(checkRuns);
        Map<Long, String> serverDisplay = checkRunService.buildServerDisplayMap(checkRuns);
        
        // 서버 상태 맵 생성 (targetName -> status)
        // 같은 targetName의 checks 중 하나라도 UP이면 UP, 모두 DOWN이면 DOWN, 그 외 UNKNOWN
        Map<String, String> serverStatusByTarget = new HashMap<>();
        for (Map.Entry<String, List<CheckEntity>> entry : checksByTarget.entrySet()) {
            String targetName = entry.getKey();
            List<CheckEntity> checks = entry.getValue();
            
            boolean hasUp = false;
            boolean hasDown = false;
            
            for (CheckEntity c : checks) {
                if (c.getStatus() != null) {
                    String status = c.getStatus().name();
                    if ("UP".equals(status)) {
                        hasUp = true;
                    } else if ("DOWN".equals(status)) {
                        hasDown = true;
                    }
                }
            }
            
            if (hasUp) {
                serverStatusByTarget.put(targetName, "UP");
            } else if (hasDown) {
                serverStatusByTarget.put(targetName, "DOWN");
            } else {
                serverStatusByTarget.put(targetName, "UNKNOWN");
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("notificationCountByCheck", notificationCountByCheck);
        result.put("checkRuns", checkRuns);
        result.put("startedDisplay", startedDisplay);
        result.put("ruleNameDisplay", ruleNameDisplay);
        result.put("serverDisplay", serverDisplay);
        result.put("serverStatusByTarget", serverStatusByTarget);
        return result;
    }
}
