package com.example.monitoring.web;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BCryptTest {

    @Test
    public void testBCrypt() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // 기존 해시
        String existingHash = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
        String password = "admin123";
        
        // 기존 해시로 검증
        boolean matches = encoder.matches(password, existingHash);
        System.out.println("기존 해시 검증 결과: " + matches);
        
        // 새 해시 생성
        String newHash = encoder.encode(password);
        System.out.println("새 해시: " + newHash);
        
        // 새 해시로 검증
        boolean newMatches = encoder.matches(password, newHash);
        System.out.println("새 해시 검증 결과: " + newMatches);
    }
}
