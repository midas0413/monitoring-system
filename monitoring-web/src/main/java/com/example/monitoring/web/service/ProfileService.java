package com.example.monitoring.web.service;

import com.example.monitoring.common.domain.UserEntity;
import com.example.monitoring.common.repo.UserRepository;
import com.example.monitoring.web.dto.ProfileForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProfileService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public ProfileForm findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(this::toForm)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + username));
    }

    public void updateProfile(ProfileForm form) {
        UserEntity entity = userRepository.findByUsername(form.getUsername())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + form.getUsername()));

        // 현재 로그인한 사용자만 수정 가능
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        if (!currentUsername.equals(form.getUsername())) {
            throw new RuntimeException("본인 계정만 수정할 수 있습니다");
        }

        // 정보 업데이트
        entity.setName(form.getName());
        entity.setDepartment(form.getDepartment());
        entity.setPosition(form.getPosition());

        // 비밀번호 변경
        if (form.getPassword() != null && !form.getPassword().trim().isEmpty()) {
            // 현재 비밀번호 확인
            if (form.getCurrentPassword() == null || form.getCurrentPassword().trim().isEmpty()) {
                throw new RuntimeException("현재 비밀번호를 입력해주세요");
            }
            
            if (!passwordEncoder.matches(form.getCurrentPassword(), entity.getPassword())) {
                throw new RuntimeException("현재 비밀번호가 올바르지 않습니다");
            }

            entity.setPassword(passwordEncoder.encode(form.getPassword()));
        }

        userRepository.save(entity);
    }

    private ProfileForm toForm(UserEntity entity) {
        ProfileForm form = new ProfileForm();
        form.setId(entity.getId());
        form.setUsername(entity.getUsername());
        form.setName(entity.getName());
        form.setDepartment(entity.getDepartment());
        form.setPosition(entity.getPosition());
        // 비밀번호는 반환하지 않음
        return form;
    }
}
