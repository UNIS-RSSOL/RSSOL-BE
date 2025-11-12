package com.example.unis_rssol.user.service;

import com.example.unis_rssol.store.entity.UserStore;
import com.example.unis_rssol.store.entity.UserStore.Position;
import com.example.unis_rssol.store.repository.UserStoreRepository;
import com.example.unis_rssol.user.dto.SelectRoleRequest;
import com.example.unis_rssol.user.dto.SelectRoleResponse;
import com.example.unis_rssol.user.entity.AppUser;
import com.example.unis_rssol.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final AppUserRepository users;
    private final UserStoreRepository userStores;
    private final UserProfileService userProfileService;


    @Transactional
    public SelectRoleResponse setRole(Long userId, SelectRoleRequest req) {
        AppUser u = users.findById(userId).orElseThrow();

        // 이미 역할이 설정되어 있으면 예외 처리
        if (!userStores.findByUserId(userId).isEmpty()) {
            throw new IllegalStateException("이미 역할이 설정된 사용자입니다.");
        }

        // user_store에 저장
        UserStore userStore = UserStore.builder()
                .user(u)
                .store(null) // 아직 매장 선택 전 (OWNER는 추후 가게 등록, STAFF는 코드 입력 시)
                .position(Position.valueOf(req.getRole().toUpperCase()))
                .employmentStatus(UserStore.EmploymentStatus.HIRED)
                .build();

        userStores.save(userStore);

        userProfileService.updateDefaultImageForRole(u, req.getRole().toUpperCase());

        return new SelectRoleResponse(u.getId(), req.getRole(), true);
    }
}
