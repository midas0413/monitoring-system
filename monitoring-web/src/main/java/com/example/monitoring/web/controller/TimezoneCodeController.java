package com.example.monitoring.web.controller;

import com.example.monitoring.common.domain.TimezoneCodeEntity;
import com.example.monitoring.common.repo.TimezoneCodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/timezones")
public class TimezoneCodeController {

    private static final Logger log = LoggerFactory.getLogger(TimezoneCodeController.class);
    private final TimezoneCodeRepository repository;

    public TimezoneCodeController(TimezoneCodeRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public String list(@RequestParam(name = "q", required = false) String q, Model model) {
        model.addAttribute("pageTitle", "타임존 코드");
        model.addAttribute("activeMenu", "settings");
        model.addAttribute("content", "timezones/list :: content");

        List<TimezoneCodeEntity> items = new ArrayList<>();
        try {
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
        } catch (Exception e) {
            // timezone_codes 테이블이 없거나 다른 오류 발생 시 빈 리스트 반환
            log.error("Failed to load timezone codes", e);
            items = new ArrayList<>();
            model.addAttribute("error", "타임존 코드를 불러오는 중 오류가 발생했습니다: " + 
                    (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
        }

        model.addAttribute("items", items);
        model.addAttribute("q", q);
        return "layout";
    }
}
