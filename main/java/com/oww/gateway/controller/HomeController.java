package com.oww.gateway.controller;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import reactor.core.publisher.Mono;

@RestController
public class HomeController {

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public Mono<String> home() {
        try {
            Resource resource = new ClassPathResource("static/index.html");
            String content = Files.readString(resource.getFile().toPath(), StandardCharsets.UTF_8);
            return Mono.just(content);
        } catch (Exception e) {
            // index.html이 없으면 간단한 HTML 반환
            String html = """
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8">
                    <title>Own Wedding Wallet</title>
                    <style>
                        body { font-family: Arial, sans-serif; text-align: center; padding: 50px; }
                        .container { max-width: 600px; margin: 0 auto; }
                        .login-btn { background: #007bff; color: white; border: none; 
                                    padding: 15px 30px; font-size: 18px; border-radius: 5px; 
                                    cursor: pointer; }
                        .login-btn:hover { background: #0056b3; }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>🏦 Own Wedding Wallet</h1>
                        <p>환영합니다! 로그인하여 서비스를 이용해보세요.</p>
                        <button class="login-btn" onclick="location.href='/oauth2/authorization/google'">
                            Google로 로그인
                        </button>
                        <br><br>
                        <a href="/banking_main">뱅킹 서비스 테스트</a>
                    </div>
                </body>
                </html>
                """;
            return Mono.just(html);
        }
    }

    @RestController
    public class AuthCheckController {
        
        @GetMapping("/auth-check")
        public ResponseEntity<Map<String, Object>> checkAuth(
                HttpServletRequest request,
                @CookieValue(value = "jwt-token", required = false) String jwtToken) {
            
            Map<String, Object> result = new HashMap<>();
            
            // 모든 쿠키 확인
            Cookie[] cookies = request.getCookies();
            Map<String, String> allCookies = new HashMap<>();
            
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    allCookies.put(cookie.getName(), 
                        cookie.getValue().length() > 50 ? 
                        cookie.getValue().substring(0, 50) + "..." : 
                        cookie.getValue());
                    
                    System.out.println("수신된 쿠키: " + cookie.getName() + 
                        " (Domain: " + cookie.getDomain() + ", Path: " + cookie.getPath() + ")");
                }
            }
            
            result.put("jwtTokenExists", jwtToken != null);
            result.put("jwtTokenValue", jwtToken != null ? 
                jwtToken.substring(0, Math.min(50, jwtToken.length())) + "..." : null);
            result.put("allCookies", allCookies);
            result.put("cookieCount", allCookies.size());
            
            System.out.println("=== 게이트웨이 쿠키 확인 ===");
            System.out.println("JWT 토큰 존재: " + (jwtToken != null));
            System.out.println("전체 쿠키 수: " + allCookies.size());
            
            return ResponseEntity.ok(result);
        }
    }
}