package com.oww.login.util;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;
    
    @Value("${jwt.expiration}")
    private Long accessTokenValidity;
    
    @Value("${jwt.refresh-expiration}")
    private Long refreshTokenValidity;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }

    // ===============================
    // 토큰 생성 (개인정보 보호 적용)
    // ===============================
    public String generateToken(Long userNo, String username, String userEmailHash) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userNo", userNo);
        claims.put("username", username);
        claims.put("userEmailHash", userEmailHash);  // 해시값 사용
        claims.put("role", "USER");

        System.out.println("JWT 토큰 생성:");
        System.out.println(" - userNo: " + userNo);
        System.out.println(" - username: " + username);
        System.out.println(" - userEmailHash: " + userEmailHash);  // 해시값 출력
        System.out.println(" - role: USER");
        System.out.println(" - secretKey 길이: " + secretKey.length());

        return createToken(claims, username, accessTokenValidity);
    }

    public String generateRefreshToken(Long userNo, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userNo", userNo);
        claims.put("username", username);
        claims.put("tokenType", "refresh");

        return createToken(claims, username, refreshTokenValidity);
    }

    private String createToken(Map<String, Object> claims, String subject, long validity) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + validity);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // ===============================
    // Claim 추출 (해시값 기반)
    // ===============================
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public String extractUserNo(String token) {
        try {
            Object userNo = extractClaim(token, claims -> claims.get("userNo"));
            return userNo != null ? userNo.toString() : null;
        } catch (Exception e) {
            System.err.println("사용자 번호 추출 오류: " + e.getMessage());
            return null;
        }
    }

    public Long extractUserNoAsLong(String token) {
        try {
            return extractClaim(token, claims -> claims.get("userNo", Long.class));
        } catch (Exception e) {
            System.err.println("사용자 번호(Long) 추출 오류: " + e.getMessage());
            return null;
        }
    }

    public String extractRole(String token) {
        try {
            String role = extractClaim(token, claims -> claims.get("role", String.class));
            return role != null ? role : "USER";
        } catch (Exception e) {
            System.err.println("역할 추출 오류: " + e.getMessage());
            return "USER";
        }
    }
    
    public String extractUserEmailHash(String token) {
        try {
            return extractClaim(token, claims -> claims.get("userEmailHash", String.class));
        } catch (Exception e) {
            System.err.println("이메일 해시 추출 오류: " + e.getMessage());
            return null;
        }
    }

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

    // ===============================
    // 토큰 검증
    // ===============================
    public Boolean validateToken(String token, String username) {
        try {
            final String extractedUsername = extractUsername(token);
            return (extractedUsername.equals(username) && !isTokenExpired(token));
        } catch (Exception e) {
            System.err.println("토큰 검증 실패: " + e.getMessage());
            return false;
        }
    }

    public Boolean validateToken(String token) {
        try {
            if (token == null || token.trim().isEmpty()) {
                System.err.println("JWT 토큰이 null이거나 비어있음");
                return false;
            }

            extractAllClaims(token);
            boolean isValid = !isTokenExpired(token);
            
            System.out.println("JWT 토큰 검증 결과: " + (isValid ? "유효" : "만료됨"));
            return isValid;
            
        } catch (Exception e) {
            System.err.println("JWT 토큰 검증 실패: " + e.getMessage());
            return false;
        }
    }

    private Boolean isTokenExpired(String token) {
        try {
            final Date expiration = extractExpiration(token);
            return expiration.before(new Date());
        } catch (Exception e) {
            System.err.println("토큰 만료 시간 확인 오류: " + e.getMessage());
            return true;
        }
    }

    // ===============================
    // 디버깅용 메서드 (개인정보 보호 적용)
    // ===============================
    public void printAllClaims(String token) {
        try {
            Claims claims = extractAllClaims(token);
            System.out.println("=== JWT 토큰 클레임 정보 ===");
            System.out.println("Subject (username): " + claims.getSubject());
            System.out.println("Issued At: " + claims.getIssuedAt());
            System.out.println("Expiration: " + claims.getExpiration());
            System.out.println("Role: " + claims.get("role"));
            System.out.println("UserNo: " + claims.get("userNo"));
            System.out.println("UserEmailHash: " + claims.get("userEmailHash"));  // 해시값 출력
            System.out.println("All Claims: " + claims);
            System.out.println("========================");
        } catch (Exception e) {
            System.err.println("클레임 출력 오류: " + e.getMessage());
        }
    }

    // ===============================
    // 토큰 유효성 및 클레임 정보를 한번에 검증하는 메서드
    // ===============================
    public TokenValidationResult validateTokenWithDetails(String token) {
        try {
            if (!validateToken(token)) {
                return new TokenValidationResult(false, "토큰이 유효하지 않거나 만료되었습니다", null, null, null, null);
            }

            String username = extractUsername(token);
            String userNo = extractUserNo(token);
            String role = extractRole(token);
            String userEmailHash = extractUserEmailHash(token);  // 해시값 추출

            return new TokenValidationResult(true, "토큰 검증 성공", username, userNo, role, userEmailHash);

        } catch (Exception e) {
            return new TokenValidationResult(false, "토큰 검증 중 오류: " + e.getMessage(), null, null, null, null);
        }
    }

    // ===============================
    // 토큰 검증 결과를 담는 클래스 (개인정보 보호 적용)
    // ===============================
    public static class TokenValidationResult {
        private final boolean valid;
        private final String message;
        private final String username;
        private final String userNo;
        private final String role;
        private final String userEmailHash;  // userEmail -> userEmailHash

        public TokenValidationResult(boolean valid, String message, String username, String userNo, String role, String userEmailHash) {
            this.valid = valid;
            this.message = message;
            this.username = username;
            this.userNo = userNo;
            this.role = role;
            this.userEmailHash = userEmailHash;  // 해시값 저장
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public String getUsername() { return username; }
        public String getUserNo() { return userNo; }
        public String getRole() { return role; }
        public String getUserEmailHash() { return userEmailHash; }  // 해시값 반환
    }
}