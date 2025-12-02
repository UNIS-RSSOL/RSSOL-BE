package com.example.unis_rssol.global.config;

import com.example.unis_rssol.domain.store.repository.UserStoreRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwt;
    private final UserStoreRepository userStoreRepository;

    public JwtAuthFilter(JwtTokenProvider jwt, UserStoreRepository userStoreRepository) {
        this.jwt = jwt;
        this.userStoreRepository = userStoreRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        log.debug("Incoming request: {}", uri);

        String header = request.getHeader("Authorization");

        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);

            if (jwt.validate(token)) {
                Long userId = jwt.getUserId(token);

                // DB에서 사용자 role 가져오기 (user_store 기반)
                String role = userStoreRepository.findFirstByUserIdOrderByCreatedAtAsc(userId)
                        .map(us -> us.getPosition().name()) // OWNER / STAFF
                        .orElse("GUEST"); // 아직 매핑 안 된 경우

                List<SimpleGrantedAuthority> authorities =
                        List.of(new SimpleGrantedAuthority("ROLE_" + role));

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(userId, null, authorities);

                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("JWT valid, userId={}, role={}", userId, role);

            } else {
                log.warn("Invalid JWT token for uri={}", uri);
            }
        } else {
            log.trace("No Authorization header for {}", uri);
        }

        filterChain.doFilter(request, response);
    }
}
