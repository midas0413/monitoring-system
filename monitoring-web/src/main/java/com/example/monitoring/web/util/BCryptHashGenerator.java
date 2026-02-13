package com.example.monitoring.web.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * BCrypt 해시 생성 유틸리티
 * 실행: java -cp "build/libs/*" com.example.monitoring.web.util.BCryptHashGenerator
 */
public class BCryptHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        String password = "admin123";
        String hash = encoder.encode(password);
        
        System.out.println("Password: " + password);
        System.out.println("BCrypt Hash: " + hash);
        System.out.println();
        System.out.println("검증 테스트:");
        System.out.println("encoder.matches(\"" + password + "\", \"" + hash + "\") = " + encoder.matches(password, hash));
    }
}
