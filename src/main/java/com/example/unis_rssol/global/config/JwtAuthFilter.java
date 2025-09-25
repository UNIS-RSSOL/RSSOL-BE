package com.example.unis_rssol.global.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;   // 로깅
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwt;

    public JwtAuthFilter(JwtTokenProvider jwt) {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        log.info("➡️ Incoming request: {}", uri);

        String header = request.getHeader("Authorization");

        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            log.info("🔑 JWT token detected: {}", token);

            if (jwt.validate(token)) {
                Long userId = jwt.getUserId(token);
                log.info("✅ Token valid for userId={}", userId);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, List.of());

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                log.warn("❌ Invalid JWT token");
            }
        } else {
            log.info("⚠️ No Authorization header provided for {}", uri);
        }

        filterChain.doFilter(request, response);
    }
}
