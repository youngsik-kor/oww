package com.oww.login.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.oww.login.entity.User;
import com.oww.login.repository.UserRepository;
import com.oww.login.util.CryptoUtil;
import com.oww.login.util.JwtUtil;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/oauth2")
@RequiredArgsConstructor
@Slf4j
public class OAuth2Controller {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @Value("${app.oauth2.redirect-uri:http://localhost:8201/?login=success}")
    private String successRedirectUri;

    // 이메일 마스킹 유틸리티 메서드
    private String maskEmail(String email) {
        if (email == null) return null;
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return "***";
        return email.substring(0, 1) + "***" + email.substring(atIndex);
    }

    @GetMapping("/success")
    public String oauth2LoginSuccess(Authentication authentication, HttpServletResponse response) {
        try {
            log.info("OAuth2 로그인 성공 처리 시작");
            
            OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
            OAuth2User oauth2User = token.getPrincipal();
            
            // OAuth2User에서 직접 정보 추출
            String email = oauth2User.getAttribute("email");
            String name = oauth2User.getAttribute("name");
            String providerId = oauth2User.getAttribute("id");
            String provider = token.getAuthorizedClientRegistrationId();
            
            // 로그에는 마스킹된 이메일만 출력
            log.info("OAuth2 사용자 정보: email={}, name={}, provider={}", 
                    maskEmail(email), name, provider);
            
            if (email == null || name == null) {
                log.error("필수 사용자 정보가 누락됨: email={}, name={}", 
                         maskEmail(email), name);
                return "redirect:" + getFailureRedirectUri("missing_user_info");
            }
            
            // DB에서 사용자 정보 조회 - 해시로 조회
            String emailHash = CryptoUtil.generateEmailHash(email);
            User user = userRepository.findByUserEmailHash(emailHash) // 메서드명 수정
                    .orElseThrow(() -> {
                        log.error("사용자를 찾을 수 없습니다: {}", maskEmail(email));
                        return new RuntimeException("사용자를 찾을 수 없습니다");
                    });
            
            // JWT 토큰 생성 - 해시값 사용
            String jwtToken = jwtUtil.generateToken(user.getUserno(), user.getName(), user.getUserEmailHash());
            String refreshToken = jwtUtil.generateRefreshToken(user.getUserno(), user.getName());
            
            log.info("JWT 토큰 생성 완료: userNo={}, userName={}", user.getUserno(), user.getName());
            
            // JWT 쿠키 설정
            setJwtCookie(response, "jwt-token", jwtToken, 24 * 60 * 60); // 24시간
            setJwtCookie(response, "refresh-token", refreshToken, 7 * 24 * 60 * 60); // 7일
            
            log.info("OAuth2 로그인 성공 완료: {} - 리다이렉트: {}", 
                    maskEmail(email), successRedirectUri);
            
            return "redirect:" + successRedirectUri;
            
        } catch (Exception e) {
            log.error("OAuth2 success 처리 중 오류: " + e.getMessage(), e);
            return "redirect:" + getFailureRedirectUri("login_error");
        }
    }

    @GetMapping("/failure")
    public String oauth2LoginFailure(Model model) {
        log.warn("OAuth2 로그인 실패");
        model.addAttribute("error", "소셜 로그인에 실패했습니다.");
        
        return "redirect:" + getFailureRedirectUri("oauth2_failure");
    }

    private void setJwtCookie(HttpServletResponse response, String name, String value, int maxAge) {
        try {
            String cookieHeader = String.format(
                "%s=%s; Path=/; Max-Age=%d; SameSite=Lax",
                name, value, maxAge
            );
            
            response.addHeader("Set-Cookie", cookieHeader);
            
            log.debug("JWT 쿠키 설정 완료: name={}, maxAge={}", name, maxAge);
            
        } catch (Exception e) {
            log.error("JWT 쿠키 설정 중 오류: " + e.getMessage(), e);
        }
    }

    private String getFailureRedirectUri(String reason) {
        return "http://localhost:8201/?login=failure&reason=" + reason;
    }

    @GetMapping("/logout")
    public String logout(HttpServletResponse response) {
        try {
            log.info("OAuth2 로그아웃 처리 시작");
            
            deleteCookie(response, "jwt-token");
            deleteCookie(response, "refresh-token");
            
            log.info("OAuth2 로그아웃 완료");
            
            return "redirect:http://localhost:8201/?logout=success";
            
        } catch (Exception e) {
            log.error("OAuth2 로그아웃 중 오류: " + e.getMessage(), e);
            return "redirect:http://localhost:8201/?logout=error";
        }
    }

    private void deleteCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, "");
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
        
        log.debug("쿠키 삭제 완료: {}", name);
    }
}