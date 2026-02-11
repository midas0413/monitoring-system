package com.example.monitoring.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/settings")
public class SettingsController {

    @GetMapping
    public String index(Model model) {
        model.addAttribute("pageTitle", "시스템 설정");
        model.addAttribute("activeMenu", "settings");
        model.addAttribute("content", "settings/index :: content");
        return "layout";
    }
}
