package com.example.monitoring.web.controller;

import com.example.monitoring.common.domain.SystemCodeEntity;
import com.example.monitoring.common.repo.SystemCodeRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.util.StringUtils;

import java.util.List;

@Controller
@RequestMapping("/settings/system-codes")
public class SystemCodeController {

    private final SystemCodeRepository repo;

    public SystemCodeController(SystemCodeRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public String list(@RequestParam(name = "type", required = false) String type,
                       @RequestParam(name = "q", required = false) String q,
                       Model model) {
        model.addAttribute("pageTitle", "시스템 코드");
        model.addAttribute("activeMenu", "settings");
        model.addAttribute("content", "settings/system-codes-list :: content");

        List<SystemCodeEntity> items;
        if (StringUtils.hasText(type)) {
            items = repo.findByCodeType(type);
        } else if (StringUtils.hasText(q)) {
            items = repo.findAll().stream()
                    .filter(code -> code.getCodeType().toLowerCase().contains(q.toLowerCase()) ||
                                   code.getCodeValue().toLowerCase().contains(q.toLowerCase()) ||
                                   (code.getCodeLabel() != null && code.getCodeLabel().toLowerCase().contains(q.toLowerCase())))
                    .sorted((a, b) -> {
                        int typeCompare = a.getCodeType().compareTo(b.getCodeType());
                        if (typeCompare != 0) return typeCompare;
                        return Integer.compare(a.getDisplayOrder(), b.getDisplayOrder());
                    })
                    .toList();
        } else {
            items = repo.findAll().stream()
                    .sorted((a, b) -> {
                        int typeCompare = a.getCodeType().compareTo(b.getCodeType());
                        if (typeCompare != 0) return typeCompare;
                        return Integer.compare(a.getDisplayOrder(), b.getDisplayOrder());
                    })
                    .toList();
        }

        List<String> codeTypes = repo.findAll().stream()
                .map(SystemCodeEntity::getCodeType)
                .distinct()
                .sorted()
                .toList();

        model.addAttribute("items", items);
        model.addAttribute("codeTypes", codeTypes);
        model.addAttribute("selectedType", type);
        model.addAttribute("q", q);
        return "layout";
    }

    @GetMapping("/new")
    public String createForm(@RequestParam(name = "type", required = false) String type, Model model) {
        model.addAttribute("pageTitle", "시스템 코드 등록");
        model.addAttribute("activeMenu", "settings");
        model.addAttribute("content", "settings/system-codes-form :: content");

        SystemCodeEntity form = new SystemCodeEntity();
        form.setEnabled(true);
        form.setDisplayOrder(0);
        if (StringUtils.hasText(type)) {
            form.setCodeType(type);
        }
        model.addAttribute("form", form);

        List<String> codeTypes = repo.findAll().stream()
                .map(SystemCodeEntity::getCodeType)
                .distinct()
                .sorted()
                .toList();
        model.addAttribute("codeTypes", codeTypes);
        return "layout";
    }

    @PostMapping("/new")
    public String create(@ModelAttribute SystemCodeEntity form, RedirectAttributes redirectAttributes) {
        try {
            repo.save(form);
            redirectAttributes.addFlashAttribute("message", "시스템 코드가 성공적으로 등록되었습니다.");
            return "redirect:/settings/system-codes" + (form.getCodeType() != null ? "?type=" + form.getCodeType() : "");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/settings/system-codes/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("pageTitle", "시스템 코드 수정");
        model.addAttribute("activeMenu", "settings");
        model.addAttribute("content", "settings/system-codes-form :: content");

        SystemCodeEntity entity = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("System code not found: " + id));
        model.addAttribute("form", entity);

        List<String> codeTypes = repo.findAll().stream()
                .map(SystemCodeEntity::getCodeType)
                .distinct()
                .sorted()
                .toList();
        model.addAttribute("codeTypes", codeTypes);
        return "layout";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute SystemCodeEntity form,
                        RedirectAttributes redirectAttributes) {
        try {
            SystemCodeEntity entity = repo.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("System code not found: " + id));
            entity.setCodeType(form.getCodeType());
            entity.setCodeValue(form.getCodeValue());
            entity.setCodeLabel(form.getCodeLabel());
            entity.setDisplayOrder(form.getDisplayOrder());
            entity.setEnabled(form.getEnabled());
            entity.setDescription(form.getDescription());
            repo.save(entity);
            redirectAttributes.addFlashAttribute("message", "시스템 코드가 성공적으로 수정되었습니다.");
            return "redirect:/settings/system-codes" + (entity.getCodeType() != null ? "?type=" + entity.getCodeType() : "");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/settings/system-codes/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            SystemCodeEntity entity = repo.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("System code not found: " + id));
            String codeType = entity.getCodeType();
            repo.deleteById(id);
            redirectAttributes.addFlashAttribute("message", "시스템 코드가 성공적으로 삭제되었습니다.");
            return "redirect:/settings/system-codes" + (codeType != null ? "?type=" + codeType : "");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/settings/system-codes";
        }
    }
}
