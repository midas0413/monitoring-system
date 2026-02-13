package com.example.monitoring.web.controller;

import com.example.monitoring.common.domain.KakaoTemplateEntity;
import com.example.monitoring.web.dto.KakaoTemplateForm;
import com.example.monitoring.web.service.KakaoTemplateService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/settings/kakao-templates")
public class KakaoTemplateController {

    private final KakaoTemplateService templateService;

    public KakaoTemplateController(KakaoTemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("pageTitle", "카카오 템플릿 관리");
        model.addAttribute("activeMenu", "settings");
        model.addAttribute("content", "settings/kakao-templates/list :: content");
        model.addAttribute("templates", templateService.list());
        return "layout";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("pageTitle", "카카오 템플릿 등록");
        model.addAttribute("activeMenu", "settings");
        model.addAttribute("content", "settings/kakao-templates/form :: content");
        model.addAttribute("form", new KakaoTemplateForm());
        return "layout";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") KakaoTemplateForm form,
                        BindingResult bindingResult,
                        RedirectAttributes redirectAttributes,
                        Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "카카오 템플릿 등록");
            model.addAttribute("activeMenu", "settings");
            model.addAttribute("content", "settings/kakao-templates/form :: content");
            return "layout";
        }

        try {
            Long id = templateService.create(form);
            redirectAttributes.addFlashAttribute("message", "카카오 템플릿이 등록되었습니다.");
            return "redirect:/settings/kakao-templates";
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("templateCode", "error.templateCode", e.getMessage());
            model.addAttribute("pageTitle", "카카오 템플릿 등록");
            model.addAttribute("activeMenu", "settings");
            model.addAttribute("content", "settings/kakao-templates/form :: content");
            return "layout";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("pageTitle", "카카오 템플릿 수정");
        model.addAttribute("activeMenu", "settings");
        model.addAttribute("content", "settings/kakao-templates/form :: content");
        KakaoTemplateEntity entity = templateService.get(id);
        model.addAttribute("form", templateService.toForm(entity));
        return "layout";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                        @Valid @ModelAttribute("form") KakaoTemplateForm form,
                        BindingResult bindingResult,
                        RedirectAttributes redirectAttributes,
                        Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "카카오 템플릿 수정");
            model.addAttribute("activeMenu", "settings");
            model.addAttribute("content", "settings/kakao-templates/form :: content");
            return "layout";
        }

        try {
            templateService.update(id, form);
            redirectAttributes.addFlashAttribute("message", "카카오 템플릿이 수정되었습니다.");
            return "redirect:/settings/kakao-templates";
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("templateCode", "error.templateCode", e.getMessage());
            model.addAttribute("pageTitle", "카카오 템플릿 수정");
            model.addAttribute("activeMenu", "settings");
            model.addAttribute("content", "settings/kakao-templates/form :: content");
            return "layout";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            templateService.delete(id);
            redirectAttributes.addFlashAttribute("message", "카카오 템플릿이 삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/settings/kakao-templates";
    }
}
