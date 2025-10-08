package com.oww.login.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;

import com.oww.login.service.CustomOAuth2UserService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    
    // 생성자 주입
    public SecurityConfig(CustomOAuth2UserService customOAuth2UserService) {
        this.customOAuth2UserService = customOAuth2UserService;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.ALWAYS)
            )
            .authorizeHttpRequests(auth -> auth
                // 공개 접근 허용
                .requestMatchers("/", "/login", "/health", "/actuator/**").permitAll()
                
                // OAuth2 관련 경로 허용
                .requestMatchers("/oauth2/authorization/**").permitAll()
                .requestMatchers("/login/oauth2/code/**").permitAll()
                .requestMatchers("/oauth2/success", "/oauth2/failure").permitAll()
                
                // API 엔드포인트 (AuthController 등)
                .requestMatchers("/api/auth/health").permitAll()
                .requestMatchers("/api/auth/**").authenticated()
                
                // 나머지는 모두 인증 필요
                .anyRequest().authenticated()
            )
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(authorization -> authorization
                    .baseUri("/oauth2/authorization")
                )
                .redirectionEndpoint(redirection -> redirection
                    .baseUri("/login/oauth2/code/*")
                )
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .defaultSuccessUrl("/oauth2/success", true)
                .failureUrl("/oauth2/failure")
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("http://localhost:8201/?logout=success")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "jwt-token", "refresh-token")
            )
            .build();
    }

    @Bean
    public HttpFirewall httpFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        firewall.setAllowSemicolon(true);
        firewall.setAllowUrlEncodedPercent(true);
        firewall.setAllowUrlEncodedPeriod(true);
        firewall.setAllowUrlEncodedSlash(true);
        return firewall;
    }

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.httpFirewall(httpFirewall());
    }
}