package com.example.monitoring.web.config;

import com.example.monitoring.common.domain.UserEntity;
import com.example.monitoring.common.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * 애플리케이션 시작 시 기본 관리자 계정 생성/업데이트
 */
@Component
public class AdminUserInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminUserInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        Optional<UserEntity> adminOpt = userRepository.findByUsername("admin");
        
        if (adminOpt.isEmpty()) {
            // admin 계정이 없으면 생성
            UserEntity admin = new UserEntity();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setName("관리자");
            admin.setDepartment("시스템관리팀");
            admin.setPosition("시스템관리자");
            admin.setRole("ADMIN");
            admin.setEnabled(true);
            userRepository.save(admin);
            log.info("기본 관리자 계정이 생성되었습니다: admin/admin123");
        } else {
            // admin 계정이 있으면 비밀번호를 올바른 해시로 업데이트
            UserEntity admin = adminOpt.get();
            String correctHash = passwordEncoder.encode("admin123");
            
            // 비밀번호가 다르면 업데이트 (BCrypt는 매번 다른 해시를 생성하므로 matches로 확인)
            if (!passwordEncoder.matches("admin123", admin.getPassword())) {
                admin.setPassword(correctHash);
                admin.setEnabled(true);
                admin.setRole("ADMIN");
                userRepository.save(admin);
                log.info("기본 관리자 계정의 비밀번호가 업데이트되었습니다: admin/admin123");
            } else {
                log.info("기본 관리자 계정이 이미 존재합니다: admin");
            }
        }
    }
}
