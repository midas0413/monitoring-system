package com.example.monitoring.web.controller;

import com.example.monitoring.common.domain.CheckRunEntity;
import com.example.monitoring.web.service.CheckRunService;
import com.example.monitoring.web.service.CheckService;
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
    private final CheckService checkService;

    public CheckRunPageController(CheckRunService checkRunService, CheckService checkService) {
        this.checkRunService = checkRunService;
        this.checkService = checkService;
    }

    @GetMapping
    public String list(
            @RequestParam(required = false) Long checkId,
            Model model) {
        model.addAttribute("pageTitle", "Check Runs");
        model.addAttribute("activeMenu", "checkRuns");
        model.addAttribute("content", "checkruns/list :: content");

        List<CheckRunEntity> items = checkRunService.list(checkId, null);
        model.addAttribute("items", items);
        model.addAttribute("startedDisplay", checkRunService.buildStartedDisplayMap(items));
        model.addAttribute("checkId", checkId);
        model.addAttribute("checks", checkService.list(null));

        return "layout";
    }
}
