package com.example.monitoring.web.controller;

import com.example.monitoring.common.domain.AlertRecipientEntity;
import com.example.monitoring.common.domain.MonitoringRuleEntity;
import com.example.monitoring.common.domain.ServerEntity;
import com.example.monitoring.web.dto.MonitoringRuleForm;
import com.example.monitoring.web.dto.RuleRecipientsForm;
import com.example.monitoring.web.service.KakaoTemplateService;
import com.example.monitoring.web.service.MessageTemplateTestService;
import com.example.monitoring.web.service.MonitoringRuleService;
import com.example.monitoring.web.service.RuleRecipientLinkService;
import com.example.monitoring.web.service.ServerService;
import com.example.monitoring.web.service.SystemCodeService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final KakaoTemplateService kakaoTemplateService;

    public MonitoringRuleController(MonitoringRuleService ruleService,
                                   ServerService serverService,
                                   SystemCodeService systemCodeService,
                                   RuleRecipientLinkService linkService,
                                   KakaoTemplateService kakaoTemplateService) {
        this.ruleService = ruleService;
        this.serverService = serverService;
        this.systemCodeService = systemCodeService;
        this.linkService = linkService;
        this.kakaoTemplateService = kakaoTemplateService;
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
        model.addAttribute("serverVpnDownMap", ruleService.buildServerVpnDownMap(items));
        
        // 수신자 수 맵 생성
        List<Long> ruleIds = items.stream().map(MonitoringRuleEntity::getId).toList();
        model.addAttribute("recipientCountMap", linkService.buildRecipientCountMap(ruleIds));
        
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
        // messageTemplate은 더 이상 사용하지 않음 (카카오 템플릿 사용)
        
        model.addAttribute("form", form);
        model.addAttribute("servers", serverService.listEnabled());
        model.addAttribute("monitoringTypes", systemCodeService.listByType("MONITORING_TYPE"));
        model.addAttribute("alertOperators", systemCodeService.listByType("ALERT_OPERATOR"));
        model.addAttribute("kakaoTemplates", kakaoTemplateService.listEnabled());
        return "layout";
    }

    @PostMapping("/new")
    public String create(@ModelAttribute MonitoringRuleForm form, RedirectAttributes redirectAttributes) {
        try {
            Long id = ruleService.create(form);
            redirectAttributes.addFlashAttribute("message", "모니터링 규칙이 성공적으로 등록되었습니다.");
            // 등록 후 목록이 아닌 수정 페이지로 이동하여 등록된 내용 확인 가능
            return "redirect:/rules/" + id + "/edit";
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
        model.addAttribute("kakaoTemplates", kakaoTemplateService.listEnabled());
        return "layout";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @ModelAttribute MonitoringRuleForm form, 
                        RedirectAttributes redirectAttributes, HttpServletRequest request) {
        try {
            // 디버깅: 폼 데이터 확인
            org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MonitoringRuleController.class);
            
            // 요청 파라미터에서 직접 값 확인 (디버깅용)
            String directParamValue = request.getParameter("kakaoTemplateVariables");
            
            // 모든 요청 파라미터 로그 출력 (디버깅용)
            log.info("===== 요청 파라미터 전체 확인 =====");
            request.getParameterMap().forEach((key, values) -> {
                if (key.contains("kakao") || key.contains("template") || key.contains("variable")) {
                    String valueStr = values != null && values.length > 0 ? values[0] : "";
                    log.info("파라미터 [{}]: 길이={}, 값={}", 
                            key, 
                            valueStr.length(),
                            valueStr.length() > 200 ? valueStr.substring(0, 200) + "..." : valueStr);
                    // 전체 값도 로그 출력 (길이 제한 없이)
                    if (key.equals("kakaoTemplateVariables")) {
                        log.info("파라미터 [{}] 전체 값: {}", key, valueStr);
                    }
                }
            });
            log.info("====================================");
            
            log.info("규칙 수정 요청: ruleId={}, templateCode={}, variablesLength={}, directParamLength={}", 
                    id, form.getKakaoTemplateCode(),
                    form.getKakaoTemplateVariables() != null ? form.getKakaoTemplateVariables().length() : 0,
                    directParamValue != null ? directParamValue.length() : 0);
            
            // 요청 파라미터 값이 있고 폼 바인딩 값과 다르면 요청 파라미터 값 사용
            if (directParamValue != null && !directParamValue.equals(form.getKakaoTemplateVariables())) {
                log.warn("⚠️ 요청 파라미터 값과 폼 바인딩 값이 다릅니다! 요청 파라미터 값을 사용합니다.");
                log.warn("폼 바인딩 값 (길이: {}): {}", 
                        form.getKakaoTemplateVariables() != null ? form.getKakaoTemplateVariables().length() : 0,
                        form.getKakaoTemplateVariables() != null && form.getKakaoTemplateVariables().length() > 100 
                            ? form.getKakaoTemplateVariables().substring(0, 100) + "..." 
                            : form.getKakaoTemplateVariables());
                log.warn("요청 파라미터 값 (길이: {}): {}", 
                        directParamValue.length(),
                        directParamValue.length() > 100 ? directParamValue.substring(0, 100) + "..." : directParamValue);
                form.setKakaoTemplateVariables(directParamValue);
            } else if (directParamValue != null && directParamValue.equals(form.getKakaoTemplateVariables())) {
                log.debug("✅ 요청 파라미터 값과 폼 바인딩 값이 일치합니다.");
            }
            
            if (form.getKakaoTemplateVariables() != null && !form.getKakaoTemplateVariables().isEmpty()) {
                log.debug("템플릿 변수 내용 (처음 200자): {}", 
                        form.getKakaoTemplateVariables().length() > 200 
                            ? form.getKakaoTemplateVariables().substring(0, 200) + "..." 
                            : form.getKakaoTemplateVariables());
            }
            
            ruleService.update(id, form);
            redirectAttributes.addFlashAttribute("message", "모니터링 규칙이 성공적으로 수정되었습니다.");
            // 수정 후 목록이 아닌 수정 페이지로 다시 이동하여 수정된 내용 확인 가능
            return "redirect:/rules/" + id + "/edit";
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

    /**
     * 카카오 템플릿 정보 조회 API (템플릿 ID로 조회)
     */
    @GetMapping("/api/kakao-template/{templateCode}")
    @ResponseBody
    public KakaoTemplateInfo getKakaoTemplateInfo(@PathVariable String templateCode) {
        var template = kakaoTemplateService.getByTemplateCode(templateCode);
        if (template == null) {
            return new KakaoTemplateInfo(null, null, null);
        }
        return new KakaoTemplateInfo(template.getTemplateCode(), template.getName(), template.getVariables());
    }

    public record KakaoTemplateInfo(String templateCode, String name, String variables) {}
}
