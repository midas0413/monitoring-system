package com.example.monitoring.web.controller;

import com.example.monitoring.common.domain.VpnConnectionEntity;
import com.example.monitoring.web.dto.VpnConnectionForm;
import com.example.monitoring.web.service.VpnConnectionService;
import com.example.monitoring.web.service.VpnNotificationTemplateService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/vpn")
public class VpnConnectionController {

    private final VpnConnectionService vpnService;
    private final VpnNotificationTemplateService templateService;

    public VpnConnectionController(VpnConnectionService vpnService,
                                  VpnNotificationTemplateService templateService) {
        this.vpnService = vpnService;
        this.templateService = templateService;
    }

    @GetMapping
    public String list(@RequestParam(name = "q", required = false) String q, Model model) {
        model.addAttribute("pageTitle", "VPN Connections");
        model.addAttribute("activeMenu", "settings");
        model.addAttribute("content", "vpn/list :: content");

        List<VpnConnectionEntity> items = vpnService.list(q);
        model.addAttribute("q", q);
        model.addAttribute("items", items);
        
        return "layout";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("pageTitle", "New VPN Connection");
        model.addAttribute("activeMenu", "settings");
        model.addAttribute("content", "vpn/form :: content");

        VpnConnectionForm form = new VpnConnectionForm();
        form.setEnabled(true);
        form.setCheckIntervalSec(60);
        model.addAttribute("form", form);
        model.addAttribute("template", null);
        model.addAttribute("templateForm", new com.example.monitoring.web.dto.VpnNotificationTemplateForm());
        return "layout";
    }

    @PostMapping
    public String create(@ModelAttribute VpnConnectionForm form, RedirectAttributes redirectAttributes) {
        try {
            Long vpnId = vpnService.create(form);
            redirectAttributes.addFlashAttribute("message", "VPN 연결 정보가 성공적으로 등록되었습니다.");
            return "redirect:/vpn/" + vpnId + "/edit";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/vpn/new";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("pageTitle", "Edit VPN Connection");
        model.addAttribute("activeMenu", "settings");
        model.addAttribute("content", "vpn/form :: content");

        VpnConnectionEntity entity = vpnService.get(id);
        VpnConnectionForm form = vpnService.toForm(entity);
        model.addAttribute("form", form);
        model.addAttribute("vpnInfo", entity); // VPN 상세 정보 전달
        // VPN당 하나의 템플릿만 사용
        var templateOpt = templateService.getByVpnId(id);
        model.addAttribute("template", templateOpt.orElse(null));
        // 템플릿 폼 객체 전달
        if (templateOpt.isPresent()) {
            model.addAttribute("templateForm", templateService.toForm(templateOpt.get()));
        } else {
            model.addAttribute("templateForm", new com.example.monitoring.web.dto.VpnNotificationTemplateForm());
        }
        return "layout";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute VpnConnectionForm form, 
                        RedirectAttributes redirectAttributes) {
        try {
            vpnService.update(id, form);
            redirectAttributes.addFlashAttribute("message", "VPN 연결 정보가 성공적으로 수정되었습니다.");
            return "redirect:/vpn/" + id + "/edit";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/vpn/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            vpnService.delete(id);
            redirectAttributes.addFlashAttribute("message", "VPN 연결 정보가 성공적으로 삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/vpn";
    }
}
