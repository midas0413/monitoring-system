package com.example.monitoring.web.controller;

import com.example.monitoring.common.domain.AlertRecipientEntity;
import com.example.monitoring.common.domain.AlertRuleEntity;
import com.example.monitoring.web.dto.AlertRuleForm;
import com.example.monitoring.web.dto.RuleRecipientsForm;
import com.example.monitoring.web.service.AlertRuleService;
import com.example.monitoring.web.service.RuleRecipientLinkService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@Controller
public class AlertRuleController {

    private final AlertRuleService ruleService;
    private final RuleRecipientLinkService linkService;

    public AlertRuleController(AlertRuleService ruleService,
                               RuleRecipientLinkService linkService) {
        this.ruleService = ruleService;
        this.linkService = linkService;
    }

    // ① rules list
    @GetMapping("/rules")
    public String list(@RequestParam(name="q", required=false) String q, Model model) {
        model.addAttribute("pageTitle", "Alert Rules");
        model.addAttribute("activeMenu", "rules");
        model.addAttribute("content", "rules/list :: content");

        List<AlertRuleEntity> items = ruleService.list(q);
        model.addAttribute("q", q);
        model.addAttribute("items", items);
        model.addAttribute("checkDisplay", ruleService.buildCheckDisplayMap(items));
        return "layout";
    }

    // ① rules new
    @GetMapping("/rules/new")
    public String createForm(Model model) {
        model.addAttribute("pageTitle", "New Rule");
        model.addAttribute("activeMenu", "rules");
        model.addAttribute("content", "rules/form :: content");

        AlertRuleForm f = new AlertRuleForm();
        f.setEnabled(true);
        f.setType("SHELL");
        f.setIntervalSec(60);
        f.setRuleType("RUN_FAILED");
        f.setCooldownSec(300);
        f.setMessageTemplate("[${targetName}] ${checkName} - output=${output}");
        model.addAttribute("form", f);
        model.addAttribute("id", null);
        return "layout";
    }

    @PostMapping("/rules/new")
    public String create(@ModelAttribute("form") AlertRuleForm form) {
        Long id = ruleService.create(form);
        return "redirect:/rules/" + id + "/edit";
    }

    // ① rules edit
    @GetMapping("/rules/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("pageTitle", "Edit Rule");
        model.addAttribute("activeMenu", "rules");
        model.addAttribute("content", "rules/form :: content");

        AlertRuleEntity e = ruleService.get(id);
        model.addAttribute("id", id);
        model.addAttribute("form", ruleService.toForm(e));
        return "layout";
    }

    @PostMapping("/rules/{id}/edit")
    public String edit(@PathVariable Long id, @ModelAttribute("form") AlertRuleForm form) {
        ruleService.update(id, form);
        return "redirect:/rules";
    }

    @PostMapping("/rules/{id}/delete")
    public String delete(@PathVariable Long id) {
        ruleService.delete(id);
        return "redirect:/rules";
    }

    // ② rule ↔ recipient link screen
    @GetMapping("/rules/{id}/recipients")
    public String recipients(@PathVariable Long id, Model model) {
        model.addAttribute("pageTitle", "Rule Recipients");
        model.addAttribute("activeMenu", "rules");
        model.addAttribute("content", "rules/recipients :: content");

        AlertRuleEntity rule = ruleService.get(id);
        List<AlertRecipientEntity> recipients = linkService.listRecipients();
        Set<Long> linkedIds = linkService.linkedRecipientIds(id);

        RuleRecipientsForm f = new RuleRecipientsForm();
        // 체크된 상태 표시용(템플릿에서 linkedIds로도 가능)
        model.addAttribute("rule", rule);
        model.addAttribute("recipients", recipients);
        model.addAttribute("linkedIds", linkedIds);
        model.addAttribute("form", f);
        return "layout";
    }

    @PostMapping("/rules/{id}/recipients")
    public String saveRecipients(@PathVariable Long id, @ModelAttribute("form") RuleRecipientsForm form) {
        linkService.saveLinks(id, form.getRecipientIds());
        return "redirect:/rules/" + id + "/recipients";
    }
}