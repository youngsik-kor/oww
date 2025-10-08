package com.oww.login.controller;

import com.oww.login.dto.AuthDto;
import com.oww.login.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @GetMapping("/me")
    public ResponseEntity<AuthDto.UserInfo> getCurrentUser(@RequestHeader("X-Username") String email) {
        try {
            AuthDto.UserInfo userInfo = authService.getUserInfo(email);
            return ResponseEntity.ok(userInfo);
        } catch (IllegalArgumentException e) {
            log.warn("사용자 정보 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Login Service is running - Social Login Only");
    }
}