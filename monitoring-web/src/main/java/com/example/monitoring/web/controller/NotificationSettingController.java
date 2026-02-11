package com.example.monitoring.web.controller;

import com.example.monitoring.common.domain.NotificationSettingEntity;
import com.example.monitoring.web.service.NotificationSettingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/settings/notification")
public class NotificationSettingController {

    private final NotificationSettingService service;

    public NotificationSettingController(NotificationSettingService service) {
        this.service = service;
    }

    @GetMapping
    public String list(@RequestParam(name = "q", required = false) String q, Model model) {
        model.addAttribute("pageTitle", "알림 설정");
        model.addAttribute("activeMenu", "settings");
        model.addAttribute("content", "settings/notification-list :: content");

        List<NotificationSettingEntity> items = service.list(q);
        model.addAttribute("q", q);
        model.addAttribute("items", items);
        return "layout";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("pageTitle", "알림 설정 등록");
        model.addAttribute("activeMenu", "settings");
        model.addAttribute("content", "settings/notification-form :: content");

        NotificationSettingEntity entity = new NotificationSettingEntity();
        entity.setEnabled(true);
        model.addAttribute("form", entity);
        return "layout";
    }

    @PostMapping("/new")
    public String create(@ModelAttribute NotificationSettingEntity form, RedirectAttributes redirectAttributes) {
        try {
            service.create(form);
            redirectAttributes.addFlashAttribute("message", "알림 설정이 성공적으로 등록되었습니다.");
            return "redirect:/settings/notification";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/settings/notification/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("pageTitle", "알림 설정 수정");
        model.addAttribute("activeMenu", "settings");
        model.addAttribute("content", "settings/notification-form :: content");

        NotificationSettingEntity entity = service.get(id);
        model.addAttribute("form", entity);
        return "layout";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute NotificationSettingEntity form, 
                        RedirectAttributes redirectAttributes) {
        try {
            service.update(id, form);
            redirectAttributes.addFlashAttribute("message", "알림 설정이 성공적으로 수정되었습니다.");
            return "redirect:/settings/notification";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/settings/notification/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            service.delete(id);
            redirectAttributes.addFlashAttribute("message", "알림 설정이 성공적으로 삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/settings/notification";
    }
}
