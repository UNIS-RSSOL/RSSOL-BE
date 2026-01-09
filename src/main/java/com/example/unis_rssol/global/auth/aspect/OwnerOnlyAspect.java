package com.example.unis_rssol.global.auth.aspect;

import com.example.unis_rssol.global.auth.AuthorizationService;
import com.example.unis_rssol.global.exception.ForbiddenException;
import com.example.unis_rssol.domain.store.entity.UserStore;
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


    @Before("@annotation(com.example.unis_rssol.global.auth.annotation.OwnerOnly) && args(storeId,..)")
    public void checkOwner(Long storeId) {
        // 활성 매장 ID 가져오기
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new ForbiddenException("로그인이 필요합니다.");
        }
        // userId와 storeId 기반 권한 체크
        UserStore requester = service.getUserStoreOrThrow(userId, storeId);
        if (requester.getPosition() != UserStore.Position.OWNER) {
            throw new ForbiddenException("해당 매장에 대한 권한이 없습니다.");
        }
    }
}