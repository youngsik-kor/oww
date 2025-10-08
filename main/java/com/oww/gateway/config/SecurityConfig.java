package com.oww.gateway.config;

import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeExchange(exchanges -> exchanges
                        // ✅ 정적 리소스 허용 (모든 서비스 공통)
                        .pathMatchers("/", "/index.html").permitAll()
                        .pathMatchers("/css/**", "/js/**", "/img/**", "/favicon.ico").permitAll()
                        .pathMatchers("/static/**").permitAll()
                        
                        // ✅ 인증 관련 경로 허용
                        .pathMatchers("/auth/**", "/login/**", "/oauth2/**").permitAll()
                        
                        // ✅ 헬스체크 허용
                        .pathMatchers("/health", "/test").permitAll()
                        
                        // ✅ Banking 경로 허용 (JWT 필터에서 인증 처리)
                        .pathMatchers("/banking/**", "/api/banking/**").permitAll()
                        
                        // ✅ Loan 경로 허용
                        .pathMatchers("/loan/**").permitAll()
                        
                        // 나머지는 인증 필요
                        .anyExchange().authenticated()
                )
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(Arrays.asList("*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
    
    @Bean
    public RouterFunction<ServerResponse> staticResourceRouter() {
        return RouterFunctions.resources("/css/**", new ClassPathResource("static/css/"))
            .and(RouterFunctions.resources("/js/**", new ClassPathResource("static/js/")))
            .and(RouterFunctions.resources("/img/**", new ClassPathResource("static/img/")))
            .and(RouterFunctions.resources("/favicon.ico", new ClassPathResource("static/favicon.ico")))
            .and(RouterFunctions.resources("/static/**", new ClassPathResource("static/")));
    }

}