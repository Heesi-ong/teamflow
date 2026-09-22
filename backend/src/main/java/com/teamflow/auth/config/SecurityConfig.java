package com.teamflow.auth.config;

import com.teamflow.auth.JwtAuthenticationFilter;
import com.teamflow.auth.JwtTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Per 09-authentication-authorization.md §1: stateless sessions,
 * JwtAuthenticationFilter registered before UsernamePasswordAuthenticationFilter,
 * only signup/login/refresh open (logout needs an authenticated principal).
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokenProvider jwtTokenProvider) throws Exception {
        http
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
}
