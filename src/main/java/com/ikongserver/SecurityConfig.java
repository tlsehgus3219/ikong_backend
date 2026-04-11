package com.ikongserver;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1. CSRF 보호 비활성화 (POST 요청 403 에러의 주범!)
            .csrf(AbstractHttpConfigurer::disable)

            // 2. Form Login 및 Http Basic 인증 비활성화
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)

            // 3. 모든 API 경로에 대해 권한 검사 없이 허용 (테스트용)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/**").permitAll() // /api로 시작하는 모든 경로 허용
                .anyRequest().authenticated()
            );

        return http.build();
    }
}