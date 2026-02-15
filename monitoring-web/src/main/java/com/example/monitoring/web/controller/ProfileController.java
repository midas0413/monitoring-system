package com.example.monitoring.web.controller;

import com.example.monitoring.web.dto.ProfileForm;
import com.example.monitoring.web.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    @Autowired
    private ProfileService profileService;

    @GetMapping
    public String profileForm(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        ProfileForm form = profileService.findByUsername(username);
        model.addAttribute("pageTitle", "내 정보");
        model.addAttribute("activeMenu", "profile");
        model.addAttribute("form", form);
        model.addAttribute("content", "profile/form :: content");
        return "layout";
    }

    @PostMapping
    public String updateProfile(
            @Valid @ModelAttribute("form") ProfileForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        // 새 비밀번호 입력 시 확인 일치 검사
        if (form.getPassword() != null && !form.getPassword().trim().isEmpty()) {
            if (form.getPasswordConfirm() == null || !form.getPassword().equals(form.getPasswordConfirm())) {
                bindingResult.rejectValue("passwordConfirm", "passwordConfirm.mismatch", "새 비밀번호와 확인이 일치하지 않습니다");
            }
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", "내 정보");
            model.addAttribute("activeMenu", "profile");
            model.addAttribute("content", "profile/form :: content");
            return "layout";
        }

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            
            // 본인 계정만 수정 가능하도록 확인
            if (!username.equals(form.getUsername())) {
                model.addAttribute("errorMessage", "본인 계정만 수정할 수 있습니다");
                model.addAttribute("pageTitle", "내 정보");
                model.addAttribute("activeMenu", "profile");
                model.addAttribute("content", "profile/form :: content");
                return "layout";
            }

            profileService.updateProfile(form);
            redirectAttributes.addFlashAttribute("successMessage", "정보가 수정되었습니다");
            return "redirect:/profile";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("pageTitle", "내 정보");
            model.addAttribute("activeMenu", "profile");
            model.addAttribute("content", "profile/form :: content");
            return "layout";
        }
    }
}
