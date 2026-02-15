package com.example.monitoring.web.controller;

import com.example.monitoring.common.domain.NotificationOutboxEntity;
import com.example.monitoring.web.service.NotificationOutboxService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/outbox")
public class NotificationOutboxPageController {

    private final NotificationOutboxService service;

    public NotificationOutboxPageController(NotificationOutboxService service) {
        this.service = service;
    }

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Long checkRunId,
            Model model) {
        model.addAttribute("pageTitle", "알림 발송 내역");
        model.addAttribute("activeMenu", "outbox");
        model.addAttribute("content", "outbox/list :: content");
        model.addAttribute("filterCheckRunId", checkRunId);

        List<NotificationOutboxEntity> items = service.list(checkRunId != null ? 0 : page, checkRunId);
        model.addAttribute("items", items);
        model.addAttribute("createdDisplay", service.buildCreatedDisplayMap(items));
        model.addAttribute("ruleDisplay", service.buildRuleDisplayMap(items));
        model.addAttribute("checkRunRuleDisplay", service.buildCheckRunRuleDisplayMap(items));
        model.addAttribute("page", page);
        model.addAttribute("hasNext", checkRunId == null && items.size() >= 100);

        return "layout";
    }
}
