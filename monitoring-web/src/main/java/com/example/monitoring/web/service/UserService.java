package com.example.monitoring.web.service;

import com.example.monitoring.common.domain.UserEntity;
import com.example.monitoring.common.repo.UserRepository;
import com.example.monitoring.web.dto.UserForm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Page<UserForm> findAll(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::toForm);
    }

    public List<UserForm> findAll() {
        return userRepository.findAll().stream()
                .map(this::toForm)
                .collect(Collectors.toList());
    }

    public UserForm findById(Long id) {
        return userRepository.findById(id)
                .map(this::toForm)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + id));
    }

    public UserForm findByUsername(String username) {
        return userRepository.findByUsername(username)
                .map(this::toForm)
                .orElse(null);
    }

    public UserForm save(UserForm form) {
        if (form.getId() != null) {
            return update(form);
        } else {
            return create(form);
        }
    }

    private UserForm create(UserForm form) {
        if (userRepository.existsByUsername(form.getUsername())) {
            throw new RuntimeException("이미 존재하는 아이디입니다: " + form.getUsername());
        }

        UserEntity entity = new UserEntity();
        applyForm(entity, form);
        entity.setPassword(passwordEncoder.encode(form.getPassword()));
        entity = userRepository.save(entity);
        return toForm(entity);
    }

    private UserForm update(UserForm form) {
        UserEntity entity = userRepository.findById(form.getId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + form.getId()));

        // 아이디 중복 체크 (자기 자신 제외)
        if (!entity.getUsername().equals(form.getUsername()) && 
            userRepository.existsByUsername(form.getUsername())) {
            throw new RuntimeException("이미 존재하는 아이디입니다: " + form.getUsername());
        }

        applyForm(entity, form);
        
        // 비밀번호가 입력된 경우에만 업데이트
        if (form.getPassword() != null && !form.getPassword().trim().isEmpty()) {
            entity.setPassword(passwordEncoder.encode(form.getPassword()));
        }

        entity = userRepository.save(entity);
        return toForm(entity);
    }

    private void applyForm(UserEntity entity, UserForm form) {
        entity.setUsername(form.getUsername());
        entity.setName(form.getName());
        entity.setDepartment(form.getDepartment());
        entity.setPosition(form.getPosition());
        entity.setRole(form.getRole());
        entity.setEnabled(form.getEnabled() != null ? form.getEnabled() : true);
    }

    private UserForm toForm(UserEntity entity) {
        UserForm form = new UserForm();
        form.setId(entity.getId());
        form.setUsername(entity.getUsername());
        form.setName(entity.getName());
        form.setDepartment(entity.getDepartment());
        form.setPosition(entity.getPosition());
        form.setRole(entity.getRole());
        form.setEnabled(entity.getEnabled());
        // 비밀번호는 반환하지 않음
        return form;
    }

    public void delete(Long id) {
        userRepository.deleteById(id);
    }
}
