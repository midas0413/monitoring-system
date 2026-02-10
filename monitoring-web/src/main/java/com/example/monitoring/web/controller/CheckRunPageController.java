package com.example.monitoring.web.controller;

import com.example.monitoring.common.domain.CheckRunEntity;
import com.example.monitoring.web.service.CheckRunService;
// import com.example.monitoring.web.service.CheckService;  // Deprecated
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
    // Deprecated: CheckService는 더 이상 사용되지 않음
    // private final CheckService checkService;

    public CheckRunPageController(CheckRunService checkRunService/*, CheckService checkService*/) {
        this.checkRunService = checkRunService;
        // this.checkService = checkService;
    }

    @GetMapping
    public String list(
            @RequestParam(required = false) Long checkId,
            Model model) {
        model.addAttribute("pageTitle", "Check Results");
        model.addAttribute("activeMenu", "checkRuns");
        model.addAttribute("content", "checkruns/list :: content");

        List<CheckRunEntity> items = checkRunService.list(checkId, null);
        model.addAttribute("items", items);
        model.addAttribute("startedDisplay", checkRunService.buildStartedDisplayMap(items));
        model.addAttribute("ruleNameDisplay", checkRunService.buildRuleNameDisplayMap(items));
        model.addAttribute("serverDisplay", checkRunService.buildServerDisplayMap(items));
        model.addAttribute("checkId", checkId);
        // Deprecated: CheckService 사용 불가
        // model.addAttribute("checks", checkService.list(null));
        model.addAttribute("checks", new java.util.ArrayList<>());

        return "layout";
    }
}
