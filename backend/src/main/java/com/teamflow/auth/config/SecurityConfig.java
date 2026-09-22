package com.teamflow.auth.config;

import com.teamflow.auth.JwtAuthenticationFilter;
import com.teamflow.auth.JwtTokenProvider;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Per 09-authentication-authorization.md §1: stateless sessions,
 * JwtAuthenticationFilter registered before UsernamePasswordAuthenticationFilter,
 * only signup/login/refresh open (logout needs an authenticated principal).
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, JwtTokenProvider jwtTokenProvider, CorsConfigurationSource corsConfigurationSource)
            throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .exceptionHandling(handling -> handling.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        // 16-monitoring-design.md: /actuator/prometheus는 nginx로 외부에 노출되지 않고
                        // Docker 내부 네트워크로만 Prometheus가 스크래핑하므로 인증 없이 허용한다.
                        .requestMatchers("/actuator/health", "/actuator/prometheus", "/api/auth/signup", "/api/auth/login", "/api/auth/refresh")
                        .permitAll()
                        // 10-realtime-architecture.md §2.1: 브라우저 WebSocket 핸드셰이크는 커스텀 헤더를
                        // 못 보내므로 HTTP 계층은 열어두고, 실제 인증은 STOMP CONNECT 프레임에서 수행한다
                        // (StompAuthChannelInterceptor).
                        .requestMatchers("/ws/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // teamflow.cors.allowed-origins가 비어 있으면(nginx 뒤 same-origin 배포, 로컬 개발의 Vite 프록시도
    // 마찬가지) 아무 매핑도 등록하지 않는다 — 직접 겪은 문제: 빈 allowedOrigins로 매핑을 등록해두면
    // Spring Security가 "이 경로는 CORS 대상"이라고 인식해서, 브라우저가 보내는 Origin 헤더(같은
    // origin으로 보이는 요청에도 POST/PUT/DELETE에는 대부분의 브라우저가 Origin을 실어 보낸다)를 보고
    // 허용 목록이 비었으니 무조건 403으로 막아버린다. 매핑 자체를 등록하지 않으면
    // getCorsConfiguration()이 모든 경로에 대해 null을 반환해서 CORS 처리 자체가 개입하지 않는다.
    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        if (corsProperties.getAllowedOrigins().isEmpty()) {
            return source;
        }

        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuration.setAllowedMethods(List.of(
                HttpMethod.GET.name(), HttpMethod.POST.name(), HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(), HttpMethod.DELETE.name(), HttpMethod.OPTIONS.name()));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        source.registerCorsConfiguration("/api/**", configuration);
        // HomePage의 백엔드 헬스체크 표시(src/pages/HomePage.tsx)가 cross-origin에서도 보이게 한다.
        source.registerCorsConfiguration("/actuator/health", configuration);
        return source;
    }
}
