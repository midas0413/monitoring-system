package com.example.monitoring.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String login(Model model) {
        model.addAttribute("pageTitle", "로그인");
        return "login";
    }

    @GetMapping("/login-error")
    public String loginError(Model model) {
        model.addAttribute("pageTitle", "로그인");
        model.addAttribute("error", true);
        model.addAttribute("errorMessage", "아이디 또는 비밀번호가 올바르지 않습니다.");
        return "login";
    }
}
