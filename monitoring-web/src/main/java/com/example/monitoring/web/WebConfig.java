package com.example.monitoring.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 모든 화면에 공통 모델 속성 추가
 * (향후 로그인 연동 시 SecurityContext에서 사용자 조회)
 */
@Configuration
public class WebConfig {

    @ControllerAdvice
    public static class GlobalModelAdvice {

        @Value("${app.system-name:Monitoring Portal}")
        private String systemName;

        @ModelAttribute("systemName")
        public String systemName() {
            return systemName;
        }

        @ModelAttribute("currentUser")
        public String currentUser() {
            // TODO: Spring Security 연동 시 SecurityContextHolder에서 조회
            return "Guest";
        }
    }
}
