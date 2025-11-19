package com.example.unis_rssol.global.config;

import com.example.unis_rssol.store.repository.UserStoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity // @PreAuthorize 사용 가능
public class SecurityConfig {

    private final JwtTokenProvider jwt;
    private final UserStoreRepository userStoreRepository;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("[SecurityConfig] SecurityFilterChain 초기화");

        return http
                .csrf(csrf -> csrf.disable())
                //  CORS 활성화: 아래 corsConfigurationSource() Bean 사용
                .cors(cors -> {})
                .authorizeHttpRequests(auth -> {
                    log.info("[SecurityConfig] 인증 예외 경로 등록");

                    // preflight(options)의 요청은 전역으로 허용해둠
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll();

                    // 로그인, 콜백, 회원가입만 허용
                    auth.requestMatchers(
                            "/",
                            "/actuator/health",
                            "/api/auth/login",
                            "/api/auth/kakao/**",
                            "/api/auth/register",
                            "/error",
                            // Swagger 관련 경로 모두 허용
                            "/swagger-ui/**",
                            "/v3/api-docs/**",
                            "/swagger-resources/**",
                            "/webjars/**",
                            "/api/auth/dev-token" //로컬개발용으로추가
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

    // CORS 규칙 정의 (dev: localhost:3000으로 우선 설정) 추가
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();

        // 허용할 프론트 개발 도메인
        cfg.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "https://rssol-fe.vercel.app"
        ));

        // 허용 메서드
        cfg.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));

        // 프론트가 보낼 헤더(현재 Authorization, Content-Type 정도)
        cfg.setAllowedHeaders(List.of("Authorization","Content-Type"));

        // 응답에서 프론트가 읽을 수 있게 노출할 헤더(필요시 추가)
        // cfg.setExposedHeaders(List.of("Location"));

        // JWT는 쿠키를 안 쓰므로 false - 쿠키 및 인증정보 요청 false로 설정
        cfg.setAllowCredentials(false);

        // Preflight 캐시(초) - 브라우저가 설정을 1시간동안 기억하게 함
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 전체 경로 적용
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
