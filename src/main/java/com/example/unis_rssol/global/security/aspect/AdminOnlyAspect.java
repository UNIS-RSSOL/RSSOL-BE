package com.example.unis_rssol.global.security.aspect;

import com.example.unis_rssol.domain.user.User;
import com.example.unis_rssol.domain.user.UserRepository;
import com.example.unis_rssol.global.exception.ForbiddenException;
import com.example.unis_rssol.global.exception.NotFoundException;
import com.example.unis_rssol.global.security.SecurityUtil;
import com.example.unis_rssol.global.security.annotation.AdminOnly;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * AdminOnly 어노테이션이 붙은 메서드에 대한 권한 검증 AOP
 * 특정 관리자 이메일(rssolewha@gmail.com)만 접근 허용
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AdminOnlyAspect {

    private static final String ADMIN_EMAIL = "rssolewha@gmail.com";
    private final UserRepository userRepository;

    @Before("@annotation(adminOnly)")
    public void checkAdmin(JoinPoint joinPoint, AdminOnly adminOnly) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new ForbiddenException("로그인이 필요합니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));

        if (!ADMIN_EMAIL.equals(user.getEmail())) {
            log.warn("🚫 [Admin] 권한 없는 접근 시도 - userId={}, email={}", userId, user.getEmail());
            throw new ForbiddenException("관리자 권한이 필요합니다.");
        }

        log.info("✅ [Admin] 권한 확인 완료 - userId={}, email={}", userId, user.getEmail());
    }
}

