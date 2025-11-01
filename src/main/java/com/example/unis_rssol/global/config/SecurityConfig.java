package com.example.unis_rssol.global.config;

import com.example.unis_rssol.store.repository.UserStoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity // @PreAuthorize 사용 가능
public class SecurityConfig {

    private final JwtTokenProvider jwt;
    private final UserStoreRepository userStoreRepository;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("[SecurityConfig] SecurityFilte\nrChain 초기화");

        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> {
                    log.info("[SecurityConfig] 인증 예외 경로 등록");

                    // 로그인, 콜백, 회원가입만 허용
                    auth.requestMatchers(
                            "/api/auth/login",
                            "/api/auth/kakao/**",
                            "/api/auth/register",
                            "/error",
                            "/auth/dev-token", //로컬개발용으로추가
                            // Swagger 관련 경로 모두 허용
                            "/swagger-ui/**",
                            "/v3/api-docs/**",
                            "/swagger-resources/**",
                            "/webjars/**"
                    ).permitAll();

                    // OWNER 전용 API
                    auth.requestMatchers("/api/auth/onboarding/owner/**").hasRole("OWNER");

                    // STAFF 전용 API
                    auth.requestMatchers("/api/auth/onboarding/staff/**").hasRole("STAFF");

                    // 나머지는 인증만 필요
                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(
                        new JwtAuthFilter(jwt, userStoreRepository),
                        UsernamePasswordAuthenticationFilter.class
                )
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
