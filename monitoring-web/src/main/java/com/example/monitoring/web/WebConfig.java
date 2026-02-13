package com.example.monitoring.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * 모든 화면에 공통 모델 속성 추가
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
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal().toString())) {
                return auth.getName();
            }
            return "Guest";
        }

        @ModelAttribute("isAdmin")
        public boolean isAdmin() {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal().toString())) {
                return auth.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            }
            return false;
        }
    }
}
