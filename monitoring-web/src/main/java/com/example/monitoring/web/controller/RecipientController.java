package com.example.monitoring.web.controller;

import com.example.monitoring.common.domain.AlertRecipientEntity;
import com.example.monitoring.web.dto.AlertRecipientForm;
import com.example.monitoring.web.service.AlertRecipientService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/recipients")
public class RecipientController {

    private final AlertRecipientService service;

    public RecipientController(AlertRecipientService service) {
        this.service = service;
    }

    @GetMapping
    public String list(@RequestParam(name = "q", required = false) String q, Model model) {
        List<AlertRecipientEntity> items = service.list(q);
        model.addAttribute("pageTitle", "Alert Recipients");
        model.addAttribute("activeMenu", "recipients");
        model.addAttribute("content", "recipients/list :: content");
        model.addAttribute("q", q);
        model.addAttribute("items", items);
        return "layout";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        AlertRecipientForm f = new AlertRecipientForm();
        model.addAttribute("pageTitle", "New Recipient");
        model.addAttribute("activeMenu", "recipients");
        model.addAttribute("content", "recipients/form :: content");
        model.addAttribute("form", f);
        model.addAttribute("id", null);
        return "layout";
    }

    @PostMapping("/new")
    public String create(@ModelAttribute("form") AlertRecipientForm form) {
        Long id = service.create(form);
        return "redirect:/recipients/" + id + "/edit";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        AlertRecipientEntity e = service.get(id);
        AlertRecipientForm f = service.toForm(e);

        model.addAttribute("pageTitle", "Edit Recipient");
        model.addAttribute("activeMenu", "recipients");
        model.addAttribute("content", "recipients/form :: content");
        model.addAttribute("id", id);
        model.addAttribute("form", f);
        return "layout";
    }

    @PostMapping("/{id}/edit")
    public String edit(@PathVariable Long id, @ModelAttribute("form") AlertRecipientForm form) {
        service.update(id, form);
        return "redirect:/recipients";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        service.delete(id);
        return "redirect:/recipients";
    }
}