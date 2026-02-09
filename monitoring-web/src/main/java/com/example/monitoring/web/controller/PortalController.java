package com.example.monitoring.web.controller;

import com.example.monitoring.common.domain.CheckEntity;
import com.example.monitoring.common.domain.CheckRunEntity;
import com.example.monitoring.web.service.CheckRunService;
import com.example.monitoring.web.service.CheckService;
import com.example.monitoring.web.service.HomeService;
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
    private final CheckService checkService;

    public PortalController(HomeService homeService, CheckRunService checkRunService, CheckService checkService) {
        this.homeService = homeService;
        this.checkRunService = checkRunService;
        this.checkService = checkService;
    }

    @GetMapping("/")
    public String home(
            @RequestParam(required = false) Long checkId,
            Model model) {
        model.addAttribute("pageTitle", "Home");
        model.addAttribute("activeMenu", "home");
        model.addAttribute("content", "home :: content");

        Map<String, List<CheckEntity>> checksByTarget = homeService.listChecksGroupByTarget();

        Map<Long, Long> notificationCountByCheck = new HashMap<>();
        for (List<CheckEntity> checks : checksByTarget.values()) {
            for (CheckEntity c : checks) {
                notificationCountByCheck.put(c.getId(), homeService.countSentNotificationsByCheck(c.getId()));
            }
        }

        model.addAttribute("checksByTarget", checksByTarget);
        model.addAttribute("notificationCountByCheck", notificationCountByCheck);

        List<CheckRunEntity> checkRuns = checkRunService.list(checkId, null);
        model.addAttribute("checkRuns", checkRuns);
        model.addAttribute("filterCheckId", checkId);
        model.addAttribute("allChecks", checkService.list(null));

        return "layout";
    }
}
