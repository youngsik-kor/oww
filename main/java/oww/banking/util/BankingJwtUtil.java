package oww.banking.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class BankingJwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration:3600000}")
    private Long expiration;

    private SecretKey key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    // ===============================
    // JWT 토큰 검증 (LoginService와 호환)
    // ===============================
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException e) {
            System.err.println("JWT 토큰 검증 실패: " + e.getMessage());
            return false;
        }
    }

    // ===============================
    // Claim 추출 (LoginService와 호환)
    // ===============================
    public String getUsernameFromToken(String token) {
        Claims claims = extractAllClaims(token);
        return claims.getSubject();
    }

    public Long extractUserNo(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("userNo", Long.class);
    }

    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("role", String.class);
    }

    // LoginService와 호환: userEmailHash 추출
    public String extractUserEmailHash(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("userEmailHash", String.class);
    }

    // 기존 메서드는 deprecated 처리
    @Deprecated
    public String extractUserEmail(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("userEmail", String.class);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // ===============================
    // 토큰 유효성 및 사용자 정보 추출
    // ===============================
    public TokenValidationResult validateAndExtract(String token) {
        try {
            if (!validateToken(token)) {
                return new TokenValidationResult(false, "유효하지 않은 토큰", null, null, null, null);
            }

            String username = getUsernameFromToken(token);
            Long userNo = extractUserNo(token);
            String role = extractRole(token);
            String userEmailHash = extractUserEmailHash(token);

            return new TokenValidationResult(true, "토큰 검증 성공", username, userNo, role, userEmailHash);

        } catch (Exception e) {
            return new TokenValidationResult(false, "토큰 처리 오류: " + e.getMessage(), null, null, null, null);
        }
    }

    // ===============================
    // 토큰 검증 결과 클래스
    // ===============================
    public static class TokenValidationResult {
        private final boolean valid;
        private final String message;
        private final String username;
        private final Long userNo;
        private final String role;
        private final String userEmailHash;

        public TokenValidationResult(boolean valid, String message, String username, Long userNo, String role, String userEmailHash) {
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
        public Long getUserNo() { return userNo; }
        public String getRole() { return role; }
        public String getUserEmailHash() { return userEmailHash; }
    }
}