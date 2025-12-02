package com.example.unis_rssol.domain.user.service;

import com.example.unis_rssol.domain.user.entity.AppUser;
import com.example.unis_rssol.domain.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private final AppUserRepository users;

    /**
     * 역할(OWNER/STAFF) 선택에 따라 기본 프로필 이미지를 업데이트합니다.
     * (사용자가 커스텀 이미지를 사용하는 경우는 건드리지 않습니다)
     */
    public void updateDefaultImageForRole(AppUser u, String newRole) {

        String currentImageUrl = u.getProfileImageUrl();

        // 기본 이미지 정책: 기본 이미지인 경우 항상 "" 로 통일
        // 즉, 커스텀 이미지가 아닌 경우 → ""
        if (currentImageUrl == null || currentImageUrl.isBlank()) {
            u.setProfileImageUrl("");
            users.save(u);
            log.info("User {} changed role to {}, using empty default profile image.",
                    u.getId(), newRole);
        }

        // 커스텀 이미지라면 아무 것도 수정하지 않음
        // (프론트에서 업로드한 이미지 그대로 둠)
    }
}
