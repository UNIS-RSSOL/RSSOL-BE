package com.example.unis_rssol.global.security.aspect;

import com.example.unis_rssol.global.security.AuthorizationService;
import com.example.unis_rssol.global.exception.ForbiddenException;
import com.example.unis_rssol.domain.store.UserStore;
import com.example.unis_rssol.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class OwnerOnlyAspect {

    private final AuthorizationService service; // 실제 AuthService 의존성


    @Before("@annotation(com.example.unis_rssol.global.security.annotation.OwnerOnly)")
    public void checkOwner() {
        // 활성 매장 ID 가져오기
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new ForbiddenException("로그인이 필요합니다.");
        }

        // 현재 활성화된 매장 ID 조회
        Long activeStoreId = service.getActiveStoreIdOrThrow(userId);

        // userId와 activeStoreId 기반 권한 체크
        UserStore requester = service.getUserStoreOrThrow(userId, activeStoreId);
        if (requester.getPosition() != UserStore.Position.OWNER) {
            throw new ForbiddenException("해당 매장에 대한 권한이 없습니다.");
        }
    }
}