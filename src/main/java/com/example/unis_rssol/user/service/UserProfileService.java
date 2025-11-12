package com.example.unis_rssol.user.service;

import com.example.unis_rssol.user.entity.AppUser;
import com.example.unis_rssol.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private final AppUserRepository users;

    // ⭐️ S3 기본 이미지 URL (UserService에서 이동)
    private static final String STAFF_DEFAULT_URL = "https://rssol-bucket.s3.ap-northeast-2.amazonaws.com/staff.svg";
    private static final String OWNER_DEFAULT_URL = "https://rssol-bucket.s3.ap-northeast-2.amazonaws.com/owner.svg";
    

    /**
     * 역할(OWNER/STAFF) 선택에 따라 기본 프로필 이미지를 업데이트합니다.
     * (사용자가 커스텀 이미지를 사용하는 경우는 건드리지 않습니다)
     */
    public void updateDefaultImageForRole(AppUser u, String newRole) {

        String currentImageUrl = u.getProfileImageUrl();

        // 현재 '기본 staff' - 선택한 역할이 'OWNER'일 경우
        if (newRole.equals("OWNER") && STAFF_DEFAULT_URL.equals(currentImageUrl)) {
            u.setProfileImageUrl(OWNER_DEFAULT_URL);
            users.save(u); // AppUser 변경 사항 저장
            log.info("User {} selected OWNER, updating profile image to default owner image.", u.getId());
        }
        else if (newRole.equals("STAFF") && OWNER_DEFAULT_URL.equals(currentImageUrl)) {
            u.setProfileImageUrl(STAFF_DEFAULT_URL);
            users.save(u); // AppUser 변경 사항 저장
            log.info("User {} selected STAFF, updating profile image to default staff image.", u.getId());
        }

    }
}
