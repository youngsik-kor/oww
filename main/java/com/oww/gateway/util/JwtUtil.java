package com.oww.gateway.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    /**
     * 토큰에서 사용자명 추출
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * 토큰 만료 시간 추출
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * 토큰에서 특정 클레임 추출
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 토큰에서 사용자 이메일 해시 추출 (LoginService와 호환)
     */
    public String extractUserEmailHash(String token) {
        try {
            return extractClaim(token, claims -> claims.get("userEmailHash", String.class));
        } catch (Exception e) {
            System.err.println("이메일 해시 추출 오류: " + e.getMessage());
            return null;
        }
    }

    /**
     * 기존 메서드 - Deprecated 처리
     */
    @Deprecated
    public String extractUserEmail(String token) {
        try {
            // LoginService에서는 userEmail 대신 userEmailHash를 사용
            String emailHash = extractUserEmailHash(token);
            if (emailHash != null) {
                System.out.println("주의: extractUserEmail 호출됨. extractUserEmailHash 사용을 권장합니다.");
                return emailHash; // 해시값을 반환하지만 메서드명이 오해의 소지
            }
            
            // 혹시 기존 토큰에 userEmail이 있다면 (하위 호환성)
            return extractClaim(token, claims -> claims.get("userEmail", String.class));
        } catch (Exception e) {
            System.err.println("이메일 추출 오류: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 토큰에서 모든 클레임 추출
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            System.err.println("JWT 토큰 만료: " + e.getMessage());
            throw new IllegalArgumentException("Token expired", e);
        } catch (UnsupportedJwtException e) {
            System.err.println("지원되지 않는 JWT 토큰: " + e.getMessage());
            throw new IllegalArgumentException("Unsupported JWT token", e);
        } catch (MalformedJwtException e) {
            System.err.println("잘못된 형식의 JWT 토큰: " + e.getMessage());
            throw new IllegalArgumentException("Malformed JWT token", e);
        } catch (SecurityException e) {
            System.err.println("JWT 서명 검증 실패: " + e.getMessage());
            throw new IllegalArgumentException("Invalid JWT signature", e);
        } catch (IllegalArgumentException e) {
            System.err.println("JWT 토큰이 비어있음: " + e.getMessage());
            throw new IllegalArgumentException("JWT token is empty", e);
        } catch (Exception e) {
            System.err.println("JWT 파싱 중 알 수 없는 오류: " + e.getMessage());
            throw new IllegalArgumentException("Invalid JWT token", e);
        }
    }

    /**
     * 토큰 만료 확인
     */
    public Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            System.err.println("토큰 만료 시간 확인 오류: " + e.getMessage());
            return true;
        }
    }

    /**
     * 토큰 검증
     */
    public Boolean validateToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                System.err.println("JWT 토큰이 null이거나 비어있음");
                return false;
            }

            // 토큰 파싱 및 서명 검증
            extractAllClaims(token);
            
            // 만료 시간 검증
            boolean isValid = !isTokenExpired(token);
            
            System.out.println("JWT 토큰 검증 결과: " + (isValid ? "유효" : "만료됨"));
            return isValid;
            
        } catch (Exception e) {
            System.err.println("JWT 토큰 검증 실패: " + e.getMessage());
            return false;
        }
    }

    /**
     * Gateway 필터에서 호출하는 메서드
     */
    public String getUsernameFromToken(String token) {
        return extractUsername(token);
    }

    /**
     * 토큰에서 역할(role) 추출
     */
    public String extractRole(String token) {
        try {
            String role = extractClaim(token, claims -> claims.get("role", String.class));
            System.out.println("추출된 역할: " + role);
            return role != null ? role : "USER";
        } catch (Exception e) {
            System.err.println("역할 추출 오류: " + e.getMessage());
            return "USER";
        }
    }

    /**
     * 토큰에서 사용자 번호 추출
     */
    public String extractUserNo(String token) {
        try {
            Object userNo = extractClaim(token, claims -> claims.get("userNo"));
            String userNoStr = userNo != null ? userNo.toString() : null;
            System.out.println("추출된 사용자 번호: " + userNoStr);
            return userNoStr;
        } catch (Exception e) {
            System.err.println("사용자 번호 추출 오류: " + e.getMessage());
            return null;
        }
    }

    /**
     * 하위 호환성을 위해 기존 메서드 유지
     */
    public Long extractUserId(String token) {
        String userNo = extractUserNo(token);
        try {
            return userNo != null ? Long.parseLong(userNo) : null;
        } catch (NumberFormatException e) {
            System.err.println("사용자 ID 변환 오류: " + e.getMessage());
            return null;
        }
    }

    /**
     * 디버깅용 - 토큰의 모든 클레임 출력
     */
    public void printAllClaims(String token) {
        try {
            Claims claims = extractAllClaims(token);
            System.out.println("=== JWT 토큰 클레임 정보 ===");
            System.out.println("Subject (username): " + claims.getSubject());
            System.out.println("Issued At: " + claims.getIssuedAt());
            System.out.println("Expiration: " + claims.getExpiration());
            System.out.println("Role: " + claims.get("role"));
            System.out.println("UserNo: " + claims.get("userNo"));
            System.out.println("UserEmailHash: " + claims.get("userEmailHash")); // 변경됨
            System.out.println("All Claims: " + claims);
            System.out.println("========================");
        } catch (Exception e) {
            System.err.println("클레임 출력 오류: " + e.getMessage());
        }
    }

    /**
     * 토큰 유효성 및 클레임 정보를 한번에 검증하는 메서드
     */
    public TokenValidationResult validateTokenWithDetails(String token) {
        try {
            if (!validateToken(token)) {
                return new TokenValidationResult(false, "토큰이 유효하지 않거나 만료되었습니다", null, null, null, null);
            }

            String username = extractUsername(token);
            String userNo = extractUserNo(token);
            String role = extractRole(token);
            String userEmailHash = extractUserEmailHash(token); // 변경됨

            return new TokenValidationResult(true, "토큰 검증 성공", username, userNo, role, userEmailHash);

        } catch (Exception e) {
            return new TokenValidationResult(false, "토큰 검증 중 오류: " + e.getMessage(), null, null, null, null);
        }
    }

    /**
     * 토큰 검증 결과를 담는 클래스 (수정됨)
     */
    public static class TokenValidationResult {
        private final boolean valid;
        private final String message;
        private final String username;
        private final String userNo;
        private final String role;
        private final String userEmailHash; // userEmail -> userEmailHash

        public TokenValidationResult(boolean valid, String message, String username, String userNo, String role, String userEmailHash) {
            this.valid = valid;
            this.message = message;
            this.username = username;
            this.userNo = userNo;
            this.role = role;
            this.userEmailHash = userEmailHash;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public String getUsername() { return username; }
        public String getUserNo() { return userNo; }
        public String getRole() { return role; }
        public String getUserEmailHash() { return userEmailHash; } // 변경됨
        
        // 하위 호환성을 위한 메서드
        @Deprecated
        public String getUserEmail() { return userEmailHash; }
    }
}