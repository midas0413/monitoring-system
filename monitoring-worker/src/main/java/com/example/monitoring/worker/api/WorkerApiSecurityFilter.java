package com.example.monitoring.worker.api;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * Worker API 보안 필터
 * API 키 기반 인증 (선택적)
 */
@Component
@Order(1)
public class WorkerApiSecurityFilter implements Filter {

    @Value("${worker.api.key:}")
    private String apiKey;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestPath = httpRequest.getRequestURI();
        
        // /api/worker 경로만 체크
        if (requestPath != null && requestPath.startsWith("/api/worker")) {
            // API 키가 설정되어 있으면 검증
            if (StringUtils.hasText(apiKey)) {
                String providedKey = httpRequest.getHeader("X-Worker-API-Key");
                if (!apiKey.equals(providedKey)) {
                    HttpServletResponse httpResponse = (HttpServletResponse) response;
                    httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    httpResponse.setContentType("application/json");
                    httpResponse.getWriter().write("{\"error\":\"Unauthorized: Invalid API Key\"}");
                    return;
                }
            }
        }
        
        chain.doFilter(request, response);
    }
}
