package com.example.monitoring.web.controller;

import com.example.monitoring.common.domain.ServerStatus;
import com.example.monitoring.common.domain.VpnConnectionEntity;
import com.example.monitoring.web.dto.VpnConnectionForm;
import com.example.monitoring.web.service.HomeService;
import com.example.monitoring.web.service.VpnConnectionService;
import com.example.monitoring.web.service.VpnNotificationTemplateService;
import com.example.monitoring.web.service.VpnRecipientLinkService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/vpn")
public class VpnConnectionController {

    private final VpnConnectionService vpnService;
    private final VpnNotificationTemplateService templateService;
    private final HomeService homeService;
    private final VpnRecipientLinkService vpnRecipientLinkService;

    public VpnConnectionController(VpnConnectionService vpnService,
                                  VpnNotificationTemplateService templateService,
                                  HomeService homeService,
                                  VpnRecipientLinkService vpnRecipientLinkService) {
        this.vpnService = vpnService;
        this.templateService = templateService;
        this.homeService = homeService;
        this.vpnRecipientLinkService = vpnRecipientLinkService;
    }

    @GetMapping
    public String list(@RequestParam(name = "q", required = false) String q, Model model) {
        model.addAttribute("pageTitle", "VPN 연결");
        model.addAttribute("activeMenu", "settings");
        model.addAttribute("content", "vpn/list :: content");

        List<VpnConnectionEntity> items = vpnService.list(q);
        List<Long> vpnIds = items.stream().map(VpnConnectionEntity::getId).toList();
        model.addAttribute("q", q);
        model.addAttribute("items", items);
        model.addAttribute("recipientCountMap", vpnRecipientLinkService.buildRecipientCountMap(vpnIds));

        return "layout";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("pageTitle", "VPN 연결 등록");
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
        model.addAttribute("pageTitle", "VPN 연결 수정");
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

    @GetMapping("/{id}/recipients")
    public String recipients(@PathVariable Long id, Model model) {
        model.addAttribute("pageTitle", "VPN 수신자 연결");
        model.addAttribute("activeMenu", "settings");
        model.addAttribute("content", "vpn/recipients :: content");

        VpnConnectionEntity vpn = vpnService.get(id);
        model.addAttribute("vpn", vpn);
        model.addAttribute("recipients", vpnRecipientLinkService.listRecipients());
        model.addAttribute("linkedIds", vpnRecipientLinkService.linkedRecipientIds(id));

        return "layout";
    }

    @PostMapping("/{id}/recipients")
    public String saveRecipients(@PathVariable Long id,
                                 @RequestParam(value = "recipientIds", required = false) List<Long> recipientIds,
                                 RedirectAttributes redirectAttributes) {
        vpnRecipientLinkService.saveLinks(id, recipientIds != null ? recipientIds : List.of());
        redirectAttributes.addFlashAttribute("message", "VPN 수신자 연결이 저장되었습니다.");
        return "redirect:/vpn/" + id + "/recipients";
    }
}

/**
 * VPN 목록 리프레시용 API
 */
@RestController
@RequestMapping("/api/vpn")
class VpnRefreshApiController {

    private final HomeService homeService;

    public VpnRefreshApiController(HomeService homeService) {
        this.homeService = homeService;
    }

    @GetMapping("/refresh")
    @ResponseBody
    public Map<String, Object> refresh() {
        // VPN 상태 목록
        List<VpnConnectionEntity> vpns = homeService.listAllVpns();
        // VPN 상태 맵을 문자열로 변환 (JavaScript에서 사용하기 위해)
        Map<Long, String> vpnStatusMap = vpns.stream()
                .collect(Collectors.toMap(
                        VpnConnectionEntity::getId,
                        vpn -> vpn.getStatus() != null ? vpn.getStatus().name() : "UNKNOWN"
                ));

        Map<String, Object> result = new HashMap<>();
        result.put("vpns", vpns);
        result.put("vpnStatusMap", vpnStatusMap);
        result.put("refresh", true);
        return result;
    }
}
