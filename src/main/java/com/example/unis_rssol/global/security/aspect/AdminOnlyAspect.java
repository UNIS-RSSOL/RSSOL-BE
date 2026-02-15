package com.example.unis_rssol.global.security.aspect;

import com.example.unis_rssol.domain.store.UserStore;
import com.example.unis_rssol.global.exception.ForbiddenException;
import com.example.unis_rssol.global.security.AuthorizationService;
import com.example.unis_rssol.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class AdminOnlyAspect {

    private final AuthorizationService service; // 실제 AuthService 의존성


    @Before("@annotation(com.example.unis_rssol.global.security.annotation.AdminOnly) && args(userId,..)")
    public void checkOwner(Long userId) {
        if (userId == null) {
            throw new ForbiddenException("로그인이 필요합니다.");
        }
        if (!service.getUserOrThrow(userId).getEmail().equals("rssol@gmail.com")){
            throw new ForbiddenException("관리자 권한이 없습니다.");
        };


    }
}
