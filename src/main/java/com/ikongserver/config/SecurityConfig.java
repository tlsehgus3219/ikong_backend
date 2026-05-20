package com.ikongserver.config;

import com.ikongserver.jwt.JwtAuthenticationFilter;
import com.ikongserver.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // JWT 기반 Stateless 인증 사용 — CSRF, 세션 불필요
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 회원가입, 로그인, 토큰 갱신은 인증 없이 허용
                        .requestMatchers("/api/v1/auth/signup", "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                        // 로그아웃은 유효한 JWT 토큰 필요
                        .requestMatchers("/api/v1/auth/logout").authenticated()
                        // 알림 생성은 IoT 기기/서버에서 호출하므로 인증 없이 허용, 조회는 보호자 JWT 필요
                        .requestMatchers(HttpMethod.POST, "/api/v1/notifications").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/notifications").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/notifications/unread-count").authenticated()
                        // 라즈베리파이 LCD 수동 알림은 디바이스 serialNum으로 식별하므로 인증 없이 허용
                        .requestMatchers(HttpMethod.POST, "/api/emergency_event/manual").permitAll()
                        // 응급 이벤트 조회/처리는 보호자 JWT 필요
                        .requestMatchers(HttpMethod.GET, "/api/emergency_event/summary").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/emergency_event/alerts").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/emergency_event/resolve-all").authenticated()
                        .requestMatchers(HttpMethod.PATCH, "/api/emergency_event/*/resolve").authenticated()
                        // 보호자 내 정보 관련 API는 JWT 필요
                        .requestMatchers("/api/v1/guardians/me/**").authenticated()
                        .anyRequest().permitAll())
                // JWT 필터를 UsernamePasswordAuthenticationFilter 앞에 삽입하여 토큰 인증 처리
                .addFilterBefore(new JwtAuthenticationFilter(jwtUtil),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
