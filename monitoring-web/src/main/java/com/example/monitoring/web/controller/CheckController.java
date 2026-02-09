package com.example.monitoring.web.controller;

import com.example.monitoring.common.repo.AlertRuleRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Checks 화면은 Alert Rules로 통합됨. 기존 URL 북마크 호환용 redirect.
 */
@Controller
@RequestMapping("/checks")
public class CheckController {

    private final AlertRuleRepository alertRuleRepository;

    public CheckController(AlertRuleRepository alertRuleRepository) {
        this.alertRuleRepository = alertRuleRepository;
    }

    @GetMapping
    public String list(RedirectAttributes ra) {
        return "redirect:/rules";
    }

    @GetMapping("/new")
    public String createForm() {
        return "redirect:/rules/new";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id) {
        return alertRuleRepository.findFirstByCheckId(id)
                .map(r -> "redirect:/rules/" + r.getId() + "/edit")
                .orElse("redirect:/rules");
    }
}