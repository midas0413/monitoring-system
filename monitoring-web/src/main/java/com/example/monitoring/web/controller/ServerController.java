package com.example.monitoring.web.controller;

import com.example.monitoring.common.domain.ServerEntity;
import com.example.monitoring.common.domain.VpnConnectionEntity;
import com.example.monitoring.web.dto.ServerForm;
import com.example.monitoring.web.service.ServerService;
import com.example.monitoring.web.service.TimezoneService;
import com.example.monitoring.web.service.VpnConnectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ServerController {

    private final ServerService serverService;
    private final TimezoneService timezoneService;
    private final VpnConnectionService vpnConnectionService;

    public ServerController(ServerService serverService,
                           TimezoneService timezoneService,
                           VpnConnectionService vpnConnectionService) {
        this.serverService = serverService;
        this.timezoneService = timezoneService;
        this.vpnConnectionService = vpnConnectionService;
    }

    @GetMapping("/servers")
    public String list(@RequestParam(name = "q", required = false) String q, Model model) {
        model.addAttribute("pageTitle", "서버");
        model.addAttribute("activeMenu", "servers");
        model.addAttribute("content", "servers/list :: content");

        List<ServerEntity> items = serverService.list(q);
        model.addAttribute("q", q);
        model.addAttribute("items", items);
        model.addAttribute("vpnDisplay", serverService.buildVpnDisplayMap(items));
        return "layout";
    }

    @GetMapping("/servers/new")
    public String createForm(Model model) {
        model.addAttribute("pageTitle", "서버 등록");
        model.addAttribute("activeMenu", "servers");
        model.addAttribute("content", "servers/form :: content");

        ServerForm form = new ServerForm();
        form.setEnabled(true);
        form.setTimezone("Asia/Seoul");
        model.addAttribute("form", form);
        model.addAttribute("timezones", timezoneService.listEnabled());
        model.addAttribute("vpnConnections", vpnConnectionService.listEnabled());
        return "layout";
    }

    @PostMapping("/servers")
    public String create(@ModelAttribute ServerForm form, RedirectAttributes redirectAttributes) {
        try {
            serverService.create(form);
            redirectAttributes.addFlashAttribute("message", "서버가 성공적으로 등록되었습니다.");
            return "redirect:/servers";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/servers/new";
        }
    }

    @GetMapping("/servers/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("pageTitle", "서버 수정");
        model.addAttribute("activeMenu", "servers");
        model.addAttribute("content", "servers/form :: content");

        ServerEntity entity = serverService.get(id);
        ServerForm form = toForm(entity);
        form.setVpnIds(serverService.getVpnIds(id));
        
        model.addAttribute("form", form);
        model.addAttribute("timezones", timezoneService.listEnabled());
        model.addAttribute("vpnConnections", vpnConnectionService.listEnabled());
        return "layout";
    }

    @PostMapping("/servers/{id}")
    public String update(@PathVariable Long id, @ModelAttribute ServerForm form, RedirectAttributes redirectAttributes) {
        try {
            serverService.update(id, form);
            redirectAttributes.addFlashAttribute("message", "서버 정보가 성공적으로 수정되었습니다.");
            return "redirect:/servers";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/servers/" + id + "/edit";
        }
    }

    @PostMapping("/servers/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            serverService.delete(id);
            redirectAttributes.addFlashAttribute("message", "서버가 성공적으로 삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/servers";
    }

    @GetMapping("/api/servers/{id}/host")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getServerHost(@PathVariable Long id) {
        try {
            ServerEntity server = serverService.get(id);
            Map<String, Object> result = new HashMap<>();
            result.put("host", server.getHost());
            result.put("sshPort", server.getSshPort());
            result.put("sshUsername", server.getSshUsername());
            result.put("sshPassword", server.getSshPassword());
            result.put("sshPrivateKeyPath", server.getSshPrivateKeyPath());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private ServerForm toForm(ServerEntity entity) {
        ServerForm form = new ServerForm();
        form.setId(entity.getId());
        form.setName(entity.getName());
        form.setHost(entity.getHost());
        form.setTimezone(entity.getTimezone());
        form.setServerPurpose(entity.getServerPurpose());
        form.setEnabled(entity.getEnabled());
        form.setDescription(entity.getDescription());
        
        // SSH 정보
        form.setSshPort(entity.getSshPort());
        form.setSshUsername(entity.getSshUsername());
        form.setSshPassword(entity.getSshPassword());
        form.setSshPrivateKeyPath(entity.getSshPrivateKeyPath());
        return form;
    }
}
