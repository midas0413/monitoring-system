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
            Model model) {
        model.addAttribute("pageTitle", "Notification Center");
        model.addAttribute("activeMenu", "outbox");
        model.addAttribute("content", "outbox/list :: content");

        List<NotificationOutboxEntity> items = service.list(page);
        model.addAttribute("items", items);
        model.addAttribute("createdDisplay", service.buildCreatedDisplayMap(items));
        model.addAttribute("page", page);
        model.addAttribute("hasNext", items.size() >= 100);

        return "layout";
    }
}
