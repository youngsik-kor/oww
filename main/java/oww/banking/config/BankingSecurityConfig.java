package oww.banking.config;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import oww.banking.util.BankingJwtUtil;

@Configuration
@EnableWebSecurity
public class BankingSecurityConfig {

    private final BankingJwtUtil jwtUtil;

    public BankingSecurityConfig(BankingJwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
        	.securityMatcher("/**") 
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .authorizeHttpRequests(auth -> auth
                    .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers("/health", "/actuator/**").permitAll()
                    .requestMatchers("/safebox/**").authenticated() 
                    .requestMatchers("/transfer/**").authenticated()     
                    .requestMatchers("/transfer").authenticated()       
                    .requestMatchers("/check-account").authenticated()   
                    .requestMatchers("/history/**").authenticated()  
                    .requestMatchers(HttpMethod.GET, "/css/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/js/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/img/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/images/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/static/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/favicon.ico").permitAll()
                    .requestMatchers(HttpMethod.GET, "/webjars/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/*.css").permitAll()
                    .requestMatchers(HttpMethod.GET, "/*.js").permitAll()
                    .requestMatchers(HttpMethod.GET, "/*.png").permitAll()
                    .requestMatchers(HttpMethod.GET, "/*.jpg").permitAll()
                    .requestMatchers(HttpMethod.GET, "/*.ico").permitAll()
                    .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((req, res, exAuth) -> {
                        res.setContentType("application/json;charset=UTF-8");
                        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        res.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"인증이 필요합니다\"}");
                    })
            )
            .addFilterBefore(new JwtAuthenticationFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    
    
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("http://localhost:8201")); // ← * 대신 명시
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true); // 쿠키 허용
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * ✅ JWT 쿠키/Authorization 헤더 직접 파싱 전용 필터
     */
    public static class JwtAuthenticationFilter extends OncePerRequestFilter {
    	   private final BankingJwtUtil jwtUtil;
    	   private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    	   public JwtAuthenticationFilter(BankingJwtUtil jwtUtil) {
    	       this.jwtUtil = jwtUtil;
    	   }

    	   @Override
    	   protected void doFilterInternal(HttpServletRequest request,
    	                                   HttpServletResponse response,
    	                                   FilterChain filterChain) throws ServletException, IOException {

    	       System.out.println("=== Banking JWT Filter 진입: " + request.getRequestURI());
    	       System.out.println("요청 방식: " + request.getMethod());

    	       String token = null;

    	       // 1. Authorization 헤더 확인
    	       String authHeader = request.getHeader("Authorization");
    	       System.out.println("Authorization 헤더: " + authHeader);
    	       
    	       if (authHeader != null && authHeader.startsWith("Bearer ")) {
    	           token = authHeader.substring(7);
    	           System.out.println("Bearer 토큰 추출 성공, 길이: " + token.length());
    	       }

    	       // 2. 쿠키에서 jwt-token 조회
    	       if (token == null && request.getCookies() != null) {
    	           System.out.println("Authorization 헤더에 토큰 없음, 쿠키 확인 중...");
    	           for (Cookie cookie : request.getCookies()) {
    	               System.out.println("쿠키 발견: " + cookie.getName() + " = " + cookie.getValue());
    	               if ("jwt-token".equals(cookie.getName())) {
    	                   token = cookie.getValue();
    	                   System.out.println("쿠키에서 JWT 토큰 추출 성공");
    	                   break;
    	               }
    	           }
    	       }

    	       if (token != null) {
    	           System.out.println("토큰 검증 시작...");
    	           try {
    	               if (jwtUtil.validateToken(token)) {
    	                   System.out.println("토큰 검증 성공!");
    	                   
    	                   String role = jwtUtil.extractRole(token);
    	                   String username = jwtUtil.getUsernameFromToken(token);
    	                   
    	                   System.out.println("추출된 사용자명: " + username);
    	                   System.out.println("추출된 역할: " + role);
    	                   
    	                   if (role == null) role = "USER";

    	                   if (SecurityContextHolder.getContext().getAuthentication() == null) {
    	                       SimpleGrantedAuthority authority =
    	                               new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role);
    	                       UsernamePasswordAuthenticationToken authToken =
    	                               new UsernamePasswordAuthenticationToken(username, null,
    	                                       Collections.singletonList(authority));
    	                       SecurityContextHolder.getContext().setAuthentication(authToken);
    	                       System.out.println("인증 컨텍스트 설정 완료");
    	                   } else {
    	                       System.out.println("이미 인증된 사용자");
    	                   }
    	               } else {
    	                   System.out.println("토큰 검증 실패!");
    	               }
    	           } catch (Exception e) {
    	               System.out.println("JWT 토큰 처리 중 오류: " + e.getMessage());
    	               e.printStackTrace();
    	           }
    	       } else {
    	           System.out.println("토큰이 없음 - 인증되지 않은 요청");
    	       }

    	       System.out.println("필터 처리 완료, 다음 필터로 진행");
    	       filterChain.doFilter(request, response);
    	   }
    	}
}
