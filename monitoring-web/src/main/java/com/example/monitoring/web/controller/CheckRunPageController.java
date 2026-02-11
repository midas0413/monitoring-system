package com.example.monitoring.web.controller;

import com.example.monitoring.common.domain.CheckRunEntity;
import com.example.monitoring.web.service.CheckRunService;
import com.example.monitoring.web.service.MonitoringRuleService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/check-runs")
public class CheckRunPageController {

    private final CheckRunService checkRunService;
    private final MonitoringRuleService monitoringRuleService;

    public CheckRunPageController(CheckRunService checkRunService,
                                  MonitoringRuleService monitoringRuleService) {
        this.checkRunService = checkRunService;
        this.monitoringRuleService = monitoringRuleService;
    }

    @GetMapping
    public String list(
            @RequestParam(required = false) Long ruleId,
            Model model) {
        model.addAttribute("pageTitle", "체크 실행 내역");
        model.addAttribute("activeMenu", "checkRuns");
        model.addAttribute("content", "checkruns/list :: content");

        List<CheckRunEntity> items = checkRunService.list(ruleId, null);
        model.addAttribute("items", items);
        model.addAttribute("startedDisplay", checkRunService.buildStartedDisplayMap(items));
        model.addAttribute("finishedDisplay", checkRunService.buildFinishedDisplayMap(items));
        model.addAttribute("ruleNameDisplay", checkRunService.buildRuleNameDisplayMap(items));
        model.addAttribute("serverDisplay", checkRunService.buildServerDisplayMap(items));
        model.addAttribute("ruleNameByRunId", checkRunService.buildRuleNameByRunIdMap(items));
        model.addAttribute("serverNameByRunId", checkRunService.buildServerNameByRunIdMap(items));
        model.addAttribute("ruleId", ruleId);
        
        // 모든 룰 목록 (필터용)
        List<com.example.monitoring.common.domain.MonitoringRuleEntity> allRules = monitoringRuleService.list(null);
        model.addAttribute("allRules", allRules);

        return "layout";
    }
}
