package com.example.monitoring.web.controller;

import com.example.monitoring.web.service.MessageTemplateTestService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

/**
 * 메시지 템플릿 테스트 API 컨트롤러
 */
@RestController
@RequestMapping("/api/monitoring-rules")
public class MessageTemplateTestController {

    private final MessageTemplateTestService templateTestService;

    public MessageTemplateTestController(MessageTemplateTestService templateTestService) {
        this.templateTestService = templateTestService;
    }

    @PostMapping(value = "/{id}/test-template", produces = MediaType.APPLICATION_JSON_VALUE)
    public MessageTemplateTestService.TestResult testTemplate(
            @PathVariable Long id,
            @RequestParam String recipient) {
        return templateTestService.testKakaoTemplate(id, recipient);
    }
}
