package com.oww.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import com.oww.gateway.util.JwtUtil;

import reactor.core.publisher.Mono;

@Component
public class JwtAuthenticationGatewayFilterFactory extends AbstractGatewayFilterFactory<JwtAuthenticationGatewayFilterFactory.Config> {

    private final JwtUtil jwtUtil;

    public JwtAuthenticationGatewayFilterFactory(JwtUtil jwtUtil) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
    }
    
    @Override
    public String name() {
        return "JwtAuth";
    }

    // 이메일 마스킹 유틸리티 메서드
    private String maskEmail(String email) {
        if (email == null) return null;
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return "***";
        return email.substring(0, 1) + "***" + email.substring(atIndex);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            System.out.println("[JWT Filter] 인증 필터 시작");
            
            ServerHttpRequest request = exchange.getRequest();
            String requestPath = request.getPath().toString();
            System.out.println("[JWT Filter] 요청 경로: " + requestPath);

            // ⭐ 정적 리소스는 JWT 검증 건너뛰기
            if (isStaticResource(requestPath)) {
                System.out.println("[JWT Filter] 정적 리소스 요청 - JWT 검증 건너뛰기: " + requestPath);
                return chain.filter(exchange);
            }
            
            // JWT 토큰 추출 (쿠키 우선, 헤더 대안)
            String jwtToken = extractJwtToken(request);
            
            if (jwtToken == null) {
                System.out.println("[JWT Filter] JWT 토큰이 없음 - 로그인 페이지로 리다이렉트");
                return redirectToLogin(exchange);
            }

            try {
                // 토큰 검증 및 사용자 정보 추출
                JwtUtil.TokenValidationResult validationResult = jwtUtil.validateTokenWithDetails(jwtToken);
                
                if (!validationResult.isValid()) {
                    System.out.println("[JWT Filter] JWT 토큰 검증 실패: " + validationResult.getMessage());
                    return redirectToLogin(exchange);
                }

                String username = validationResult.getUsername();
                String userNo = validationResult.getUserNo();
                String role = validationResult.getRole();
                String userEmailHash = validationResult.getUserEmailHash(); // 해시값 추출

                System.out.println("[JWT Filter] JWT 토큰 검증 성공");
                System.out.println("   - Username: " + username);
                System.out.println("   - Role: " + role);
                System.out.println("   - UserNo: " + userNo);
                System.out.println("   - UserEmailHash: " + (userEmailHash != null ? userEmailHash.substring(0, 8) + "..." : "null"));

                // Banking Service로 전달할 헤더 설정
                ServerWebExchange mutatedExchange = exchange.mutate()
                	    .request(r -> r
                	        .header("Authorization", "Bearer " + jwtToken) // JWT 헤더 전달
                	        .header("x-user-no", userNo != null ? userNo : "")
                	        .header("x-username", username != null ? username : "")
                	        .header("x-user-role", role != null ? ("ROLE_" + role) : "ROLE_USER")
                	        .header("x-user-email-hash", userEmailHash != null ? userEmailHash : "")
                	    )
                	    .build();


                System.out.println("[JWT Filter] 헤더 추가 완료 - Banking Service로 JWT + 해시 전달");
                System.out.println("   - Authorization: Bearer ***");
                System.out.println("   - x-user-email-hash: " + (userEmailHash != null ? userEmailHash.substring(0, 8) + "..." : "null"));

                return chain.filter(mutatedExchange);

            } catch (Exception e) {
                System.err.println("[JWT Filter] JWT 토큰 처리 중 예외 발생: " + e.getMessage());
                e.printStackTrace();
                return redirectToLogin(exchange);
            }
        };
    }

    /**
     * JWT 토큰 추출 (쿠키 우선, Authorization 헤더 대안)
     */
    private String extractJwtToken(ServerHttpRequest request) {
        // 1. 쿠키에서 jwt-token 확인
        String cookieToken = request.getCookies()
                .getFirst("jwt-token") != null ?
                request.getCookies()
                        .getFirst("jwt-token")
                        .getValue() : null;

        if (cookieToken != null && !cookieToken.trim().isEmpty()) {
            System.out.println("[JWT Filter] 쿠키에서 JWT 토큰 발견");
            return cookieToken.trim();
        }

        // 2. Authorization 헤더에서 확인
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (!token.isEmpty()) {
                System.out.println("[JWT Filter] Authorization 헤더에서 JWT 토큰 발견");
                return token;
            }
        }

        System.out.println("[JWT Filter] JWT 토큰을 찾을 수 없음");
        return null;
    }

    
    /**
     * 정적 리소스 요청인지 확인
     */
    private boolean isStaticResource(String path) {
        return path.startsWith("/css/") ||
               path.startsWith("/js/") ||
               path.startsWith("/img/") ||
               path.startsWith("/images/") ||
               path.startsWith("/static/") ||
               path.startsWith("/webjars/") ||
               path.equals("/favicon.ico") ||
               path.endsWith(".css") ||
               path.endsWith(".js") ||
               path.endsWith(".png") ||
               path.endsWith(".jpg") ||
               path.endsWith(".jpeg") ||
               path.endsWith(".gif") ||
               path.endsWith(".ico") ||
               path.endsWith(".svg") ||
               path.endsWith(".woff") ||
               path.endsWith(".woff2") ||
               path.endsWith(".ttf") ||
               path.endsWith(".eot");
    }
    
    
    /**
     * 로그인 페이지로 리다이렉트
     */
    private Mono<Void> redirectToLogin(ServerWebExchange exchange) {
        String originalPath = exchange.getRequest().getPath().toString();
        String redirectUrl = "/auth/oauth2/authorization/google";
        
        // API 요청인 경우 JSON 에러 응답
        if (originalPath.startsWith("/api/")) {
            return createJsonErrorResponse(exchange, "Authentication required", HttpStatus.UNAUTHORIZED);
        }
        
        // 일반 페이지 요청인 경우 로그인 페이지로 리다이렉트
        System.out.println("[JWT Filter] 로그인 페이지로 리다이렉트: " + redirectUrl);
        
        exchange.getResponse().setStatusCode(HttpStatus.FOUND);
        exchange.getResponse().getHeaders().add("Location", redirectUrl);
        return exchange.getResponse().setComplete();
    }

    /**
     * JSON 에러 응답 (API 요청용)
     */
    private Mono<Void> createJsonErrorResponse(ServerWebExchange exchange, String errorMessage, HttpStatus status) {
        System.out.println("[JWT Filter] JSON 에러 응답: " + errorMessage);
        
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json");

        String body = String.format(
            "{\"success\": false, \"error\": \"%s\", \"message\": \"%s\", \"timestamp\": \"%s\"}",
            status.getReasonPhrase(), errorMessage, java.time.Instant.now()
        );

        return exchange.getResponse().writeWith(
                Mono.just(exchange.getResponse().bufferFactory().wrap(body.getBytes()))
        );
    }

    public static class Config {
        private boolean redirectOnFailure = true;
        private boolean debugMode = false;
        
        public boolean isRedirectOnFailure() {
            return redirectOnFailure;
        }
        
        public void setRedirectOnFailure(boolean redirectOnFailure) {
            this.redirectOnFailure = redirectOnFailure;
        }
        
        public boolean isDebugMode() {
            return debugMode;
        }
        
        public void setDebugMode(boolean debugMode) {
            this.debugMode = debugMode;
        }
    }
}