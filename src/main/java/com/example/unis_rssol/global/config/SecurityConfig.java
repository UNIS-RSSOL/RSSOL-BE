package com.example.unis_rssol.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtTokenProvider jwt;

    public SecurityConfig(JwtTokenProvider jwt) {
        this.jwt = jwt;
    }

    @Bean
    public SecurityFilterChain filter(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(reg -> reg
                        .requestMatchers("/api/auth/**").permitAll() // 로그인/콜백/토큰 재발급 모두 허용
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthFilter(jwt), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
