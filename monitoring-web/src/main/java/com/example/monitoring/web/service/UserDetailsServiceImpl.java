package com.example.monitoring.web.service;

import com.example.monitoring.common.domain.UserEntity;
import com.example.monitoring.common.repo.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("사용자 조회 시도: {}", username);
        
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("사용자를 찾을 수 없습니다: {}", username);
                    return new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username);
                });

        if (!user.getEnabled()) {
            log.warn("비활성화된 사용자 로그인 시도: {}", username);
            throw new UsernameNotFoundException("비활성화된 사용자입니다: " + username);
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole()));

        log.debug("사용자 로드 성공: {}, 역할: {}", username, user.getRole());

        return User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!user.getEnabled())
                .build();
    }
}
