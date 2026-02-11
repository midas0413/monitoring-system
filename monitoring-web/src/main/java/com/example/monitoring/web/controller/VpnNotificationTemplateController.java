package com.example.monitoring.web.controller;

import com.example.monitoring.web.dto.VpnNotificationTemplateForm;
import com.example.monitoring.web.service.VpnNotificationTemplateService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/vpn/{vpnId}/templates")
public class VpnNotificationTemplateController {

    private final VpnNotificationTemplateService templateService;

    public VpnNotificationTemplateController(VpnNotificationTemplateService templateService) {
        this.templateService = templateService;
    }

    @PostMapping
    public String create(@PathVariable Long vpnId, @ModelAttribute VpnNotificationTemplateForm form,
                        RedirectAttributes redirectAttributes) {
        try {
            form.setVpnId(vpnId);
            templateService.create(form);
            redirectAttributes.addFlashAttribute("message", "VPN 알림 템플릿이 성공적으로 등록되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/vpn/" + vpnId + "/edit";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long vpnId, @PathVariable Long id,
                        @ModelAttribute VpnNotificationTemplateForm form,
                        RedirectAttributes redirectAttributes) {
        try {
            form.setVpnId(vpnId);
            templateService.update(id, form);
            redirectAttributes.addFlashAttribute("message", "VPN 알림 템플릿이 성공적으로 수정되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/vpn/" + vpnId + "/edit";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long vpnId, @PathVariable Long id,
                        RedirectAttributes redirectAttributes) {
        try {
            templateService.delete(id);
            redirectAttributes.addFlashAttribute("message", "VPN 알림 템플릿이 성공적으로 삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/vpn/" + vpnId + "/edit";
    }
}
