package com.oww.login.service;

import com.oww.login.dto.AuthDto;
import com.oww.login.entity.User;
import com.oww.login.repository.UserRepository;
import com.oww.login.util.CryptoUtil;
import com.oww.login.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    // 이메일 마스킹 유틸리티 메서드
    private String maskEmail(String email) {
        if (email == null) return null;
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return "***";
        return email.substring(0, 1) + "***" + email.substring(atIndex);
    }

    @Transactional(readOnly = true)
    public AuthDto.UserInfo getUserInfo(String email) {
        // 이메일을 해시로 변환해서 검색
        String emailHash = CryptoUtil.generateEmailHash(email);
        User user = userRepository.findByUserEmailHashAndIsActiveTrue(emailHash) // 메서드명 수정
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return AuthDto.UserInfo.from(user);
    }

    public AuthDto.SocialLoginResponse processSocialLogin(User user, boolean isNewUser) {
        // JWT 토큰 생성 시 해시된 이메일 사용
        String accessToken = jwtUtil.generateToken(user.getUserno(), user.getName(), user.getUserEmailHash());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUserno(), user.getName());

        // 로그에는 마스킹된 이메일만 출력
        log.info("소셜 로그인 처리: {} ({}) - 신규사용자: {}",
                maskEmail(user.getUserEmail()), user.getProvider(), isNewUser);

        return AuthDto.SocialLoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userInfo(AuthDto.UserInfo.from(user))
                .isNewUser(isNewUser)
                .build();
    }

    // 이메일로 사용자 찾기 헬퍼 메서드
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        String emailHash = CryptoUtil.generateEmailHash(email);
        return userRepository.findByUserEmailHash(emailHash) // 메서드명 수정
                .orElse(null);
    }

    // 이메일 존재 여부 확인
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        String emailHash = CryptoUtil.generateEmailHash(email);
        return userRepository.existsByUserEmailHash(emailHash); // 메서드명 수정
    }

    // 해시로 직접 사용자 조회 (권장 방식)
    @Transactional(readOnly = true)
    public User findByEmailHash(String emailHash) {
        return userRepository.findByUserEmailHash(emailHash)
                .orElse(null);
    }

    // 해시로 활성 사용자 조회
    @Transactional(readOnly = true)
    public User findActiveUserByEmailHash(String emailHash) {
        return userRepository.findByUserEmailHashAndIsActiveTrue(emailHash)
                .orElse(null);
    }
}