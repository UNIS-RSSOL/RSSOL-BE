package com.example.unis_rssol.global.security.aspect;

import com.example.unis_rssol.global.security.AuthorizationService;
import com.example.unis_rssol.global.exception.ForbiddenException;
import com.example.unis_rssol.domain.store.UserStore;
import com.example.unis_rssol.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class OwnerOnlyAspect {

    private final AuthorizationService service;

    @Before("@annotation(com.example.unis_rssol.global.security.annotation.OwnerOnly)")
    public void checkOwner(JoinPoint joinPoint) {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            throw new ForbiddenException("로그인이 필요합니다.");
        }
        Long storeId = resolveStoreId(joinPoint, userId);
        UserStore requester = service.getUserStoreOrThrow(userId, storeId);
        if (requester.getPosition() != UserStore.Position.OWNER) {
            throw new ForbiddenException("해당 매장에 대한 권한이 없습니다.");
        }
    }

    /**
     * 메서드 파라미터 중 이름이 "storeId"인 Long 인자를 찾아 반환한다.
     * 해당 파라미터가 없으면 사용자의 활성 매장 ID를 반환한다.
     *
     * <p>파라미터 이름 조회는 AspectJ의 {@link MethodSignature#getParameterNames()}를 사용하며,
     * 바이트코드의 디버그 정보(로컬 변수 테이블)를 읽는다.
     * Gradle/Maven 기본 빌드 옵션에서는 디버그 정보가 포함되므로 별도 설정이 필요하지 않다.
     */
    private Long resolveStoreId(JoinPoint joinPoint, Long userId) {
        if (!(joinPoint.getSignature() instanceof MethodSignature methodSignature)) {
            return service.getActiveStoreIdOrThrow(userId);
        }

        String[] parameterNames = methodSignature.getParameterNames();
        Object[] args = joinPoint.getArgs();

        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                if ("storeId".equals(parameterNames[i]) && args[i] instanceof Long storeId) {
                    return storeId;
                }
            }
        }

        return service.getActiveStoreIdOrThrow(userId);
    }
}