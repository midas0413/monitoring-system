package com.example.monitoring.web.controller;

import com.example.monitoring.web.dto.UserForm;
import com.example.monitoring.web.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/settings/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public String list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Model model) {
        Pageable pageable = PageRequest.of(page, size);
        Page<UserForm> users = userService.findAll(pageable);
        
        model.addAttribute("pageTitle", "사용자 관리");
        model.addAttribute("activeMenu", "users");
        model.addAttribute("users", users);
        model.addAttribute("content", "users/list :: content");
        return "layout";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        UserForm form = new UserForm();
        model.addAttribute("pageTitle", "사용자 등록");
        model.addAttribute("activeMenu", "users");
        model.addAttribute("form", form);
        model.addAttribute("content", "users/form :: content");
        return "layout";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        UserForm form = userService.findById(id);
        model.addAttribute("pageTitle", "사용자 수정");
        model.addAttribute("activeMenu", "users");
        model.addAttribute("form", form);
        model.addAttribute("content", "users/form :: content");
        return "layout";
    }

    @PostMapping
    public String save(
            @Valid @ModelAttribute("form") UserForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("pageTitle", form.getId() == null ? "사용자 등록" : "사용자 수정");
            model.addAttribute("activeMenu", "users");
            model.addAttribute("content", "users/form :: content");
            return "layout";
        }

        try {
            if (form.getId() == null && (form.getPassword() == null || form.getPassword().trim().isEmpty())) {
                bindingResult.rejectValue("password", "required", "비밀번호는 필수입니다");
                model.addAttribute("pageTitle", "사용자 등록");
                model.addAttribute("activeMenu", "users");
                model.addAttribute("content", "users/form :: content");
                return "layout";
            }

            userService.save(form);
            redirectAttributes.addFlashAttribute("successMessage", 
                form.getId() == null ? "사용자가 등록되었습니다" : "사용자 정보가 수정되었습니다");
            return "redirect:/settings/users";
        } catch (Exception e) {
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("pageTitle", form.getId() == null ? "사용자 등록" : "사용자 수정");
            model.addAttribute("activeMenu", "users");
            model.addAttribute("content", "users/form :: content");
            return "layout";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", "사용자가 삭제되었습니다");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
        return "redirect:/settings/users";
    }
}
