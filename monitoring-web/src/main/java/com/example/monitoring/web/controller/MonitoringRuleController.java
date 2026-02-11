package com.example.monitoring.web.controller;

import com.example.monitoring.common.domain.AlertRecipientEntity;
import com.example.monitoring.common.domain.MonitoringRuleEntity;
import com.example.monitoring.common.domain.ServerEntity;
import com.example.monitoring.web.dto.MonitoringRuleForm;
import com.example.monitoring.web.dto.RuleRecipientsForm;
import com.example.monitoring.web.service.MonitoringRuleService;
import com.example.monitoring.web.service.RuleRecipientLinkService;
import com.example.monitoring.web.service.ServerService;
import com.example.monitoring.web.service.SystemCodeService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;

@Controller
@RequestMapping({"/monitoring-rules", "/rules"})  // /rules는 하위 호환성을 위해 추가
public class MonitoringRuleController {

    private final MonitoringRuleService ruleService;
    private final ServerService serverService;
    private final SystemCodeService systemCodeService;
    private final RuleRecipientLinkService linkService;

    public MonitoringRuleController(MonitoringRuleService ruleService,
                                   ServerService serverService,
                                   SystemCodeService systemCodeService,
                                   RuleRecipientLinkService linkService) {
        this.ruleService = ruleService;
        this.serverService = serverService;
        this.systemCodeService = systemCodeService;
        this.linkService = linkService;
    }

    @GetMapping
    public String list(@RequestParam(name = "q", required = false) String q, Model model) {
        model.addAttribute("pageTitle", "알림 규칙");
        model.addAttribute("activeMenu", "rules");
        model.addAttribute("content", "monitoring-rules/list :: content");

        List<MonitoringRuleEntity> items = ruleService.list(q);
        model.addAttribute("q", q);
        model.addAttribute("items", items);
        model.addAttribute("serverDisplay", ruleService.buildServerDisplayMap(items));
        return "layout";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("pageTitle", "알림 규칙 등록");
        model.addAttribute("activeMenu", "rules");
        model.addAttribute("content", "monitoring-rules/form :: content");

        MonitoringRuleForm form = new MonitoringRuleForm();
        form.setEnabled(true);
        form.setIntervalSec(60);
        form.setCooldownSec(300);
        form.setMessageTemplate("[${ruleName}] 모니터링에 알림 발생.\n임계값 : ${threshold}\n현재값 : ${outputNum}\n발생시각 : ${finishedAt} [${finishedAtLocal}]");
        
        model.addAttribute("form", form);
        model.addAttribute("servers", serverService.listEnabled());
        model.addAttribute("monitoringTypes", systemCodeService.listByType("MONITORING_TYPE"));
        model.addAttribute("alertOperators", systemCodeService.listByType("ALERT_OPERATOR"));
        return "layout";
    }

    @PostMapping("/new")
    public String create(@ModelAttribute MonitoringRuleForm form, RedirectAttributes redirectAttributes) {
        try {
            ruleService.create(form);
            redirectAttributes.addFlashAttribute("message", "모니터링 규칙이 성공적으로 등록되었습니다.");
            return "redirect:/rules";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/rules/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("pageTitle", "알림 규칙 수정");
        model.addAttribute("activeMenu", "rules");
        model.addAttribute("content", "monitoring-rules/form :: content");

        MonitoringRuleEntity entity = ruleService.get(id);
        MonitoringRuleForm form = ruleService.toForm(entity);
        
        model.addAttribute("form", form);
        model.addAttribute("servers", serverService.listEnabled());
        model.addAttribute("monitoringTypes", systemCodeService.listByType("MONITORING_TYPE"));
        model.addAttribute("alertOperators", systemCodeService.listByType("ALERT_OPERATOR"));
        return "layout";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute MonitoringRuleForm form, 
                        RedirectAttributes redirectAttributes) {
        try {
            ruleService.update(id, form);
            redirectAttributes.addFlashAttribute("message", "모니터링 규칙이 성공적으로 수정되었습니다.");
            return "redirect:/rules";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/rules/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            ruleService.delete(id);
            redirectAttributes.addFlashAttribute("message", "모니터링 규칙이 성공적으로 삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/rules";
    }

    @GetMapping("/{id}/recipients")
    public String recipients(@PathVariable Long id, Model model) {
        model.addAttribute("pageTitle", "규칙 수신자");
        model.addAttribute("activeMenu", "rules");
        model.addAttribute("content", "monitoring-rules/recipients :: content");

        MonitoringRuleEntity rule = ruleService.get(id);
        List<AlertRecipientEntity> recipients = linkService.listRecipients();
        Set<Long> linkedIds = linkService.linkedRecipientIds(id);

        RuleRecipientsForm form = new RuleRecipientsForm();
        model.addAttribute("form", form);
        model.addAttribute("rule", rule);
        model.addAttribute("recipients", recipients);
        model.addAttribute("linkedIds", linkedIds);
        return "layout";
    }

    @PostMapping("/{id}/recipients")
    public String saveRecipients(@PathVariable Long id, @ModelAttribute("form") RuleRecipientsForm form,
                                RedirectAttributes redirectAttributes) {
        try {
            linkService.saveLinks(id, form.getRecipientIds());
            redirectAttributes.addFlashAttribute("message", "수신자 연결이 성공적으로 저장되었습니다.");
            return "redirect:/rules/" + id + "/recipients";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "수신자 연결 저장 중 오류가 발생했습니다: " + e.getMessage());
            return "redirect:/rules/" + id + "/recipients";
        }
    }
}
