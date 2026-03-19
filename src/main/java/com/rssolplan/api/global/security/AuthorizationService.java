package com.rssolplan.api.global.security;

import com.rssolplan.api.domain.store.UserStore;
import com.rssolplan.api.domain.store.UserStoreRepository;
import com.rssolplan.api.domain.user.User;
import com.rssolplan.api.domain.user.UserRepository;
import com.rssolplan.api.global.exception.ForbiddenException;
import com.rssolplan.api.global.exception.NotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AuthorizationService {

    private final UserRepository appUserRepository;
    private final UserStoreRepository userStoreRepository;

    public AuthorizationService(UserRepository appUserRepository,
                                UserStoreRepository userStoreRepository) {
        this.appUserRepository = appUserRepository;
        this.userStoreRepository = userStoreRepository;
    }

    /**
     * 현재 활성화된 매장 ID 조회
     */
    public Long getActiveStoreIdOrThrow(Long userId) {
        User user = appUserRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("존재하지 않는 사용자입니다."));
        Long activeStoreId = user.getActiveStoreId();
        if (activeStoreId == null) {
            throw new ForbiddenException("현재 활성화된 매장이 없습니다.");
        }
        return activeStoreId;
    }

    /**
     * 사용자와 매장 권한 체크
     */
    public UserStore getUserStoreOrThrow(Long userId, Long storeId) {
        return userStoreRepository.findByUser_IdAndStore_Id(userId, storeId)
                .orElseThrow(() -> new ForbiddenException("해당 매장에 근무할 권한이 없습니다."));
    }
}