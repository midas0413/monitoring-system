package com.example.monitoring.web.controller;

import com.example.monitoring.common.domain.TimezoneCodeEntity;
import com.example.monitoring.common.repo.TimezoneCodeRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/timezones")
public class TimezoneCodeController {

    private final TimezoneCodeRepository repository;

    public TimezoneCodeController(TimezoneCodeRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public String list(@RequestParam(name = "q", required = false) String q, Model model) {
        model.addAttribute("pageTitle", "Timezone Codes");
        model.addAttribute("activeMenu", "settings");
        model.addAttribute("content", "timezones/list :: content");

        List<TimezoneCodeEntity> items;
        if (q != null && !q.trim().isEmpty()) {
            String searchTerm = q.toLowerCase().trim();
            items = repository.findAll().stream()
                    .filter(tz -> (tz.getTimezoneId() != null && tz.getTimezoneId().toLowerCase().contains(searchTerm)) ||
                                 (tz.getDisplayName() != null && tz.getDisplayName().toLowerCase().contains(searchTerm)) ||
                                 (tz.getDescription() != null && tz.getDescription().toLowerCase().contains(searchTerm)))
                    .sorted((a, b) -> {
                        int enabledCompare = Boolean.compare(!a.getEnabled(), !b.getEnabled());
                        if (enabledCompare != 0) return enabledCompare;
                        int orderCompare = Integer.compare(a.getDisplayOrder(), b.getDisplayOrder());
                        if (orderCompare != 0) return orderCompare;
                        return a.getTimezoneId().compareTo(b.getTimezoneId());
                    })
                    .toList();
        } else {
            items = repository.findAll().stream()
                    .sorted((a, b) -> {
                        int enabledCompare = Boolean.compare(!a.getEnabled(), !b.getEnabled());
                        if (enabledCompare != 0) return enabledCompare;
                        int orderCompare = Integer.compare(a.getDisplayOrder(), b.getDisplayOrder());
                        if (orderCompare != 0) return orderCompare;
                        return a.getTimezoneId().compareTo(b.getTimezoneId());
                    })
                    .toList();
        }

        model.addAttribute("items", items);
        model.addAttribute("q", q);
        return "layout";
    }
}
