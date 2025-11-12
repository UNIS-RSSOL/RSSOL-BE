package com.example.unis_rssol.user.service;

import com.example.unis_rssol.user.entity.AppUser;
import com.example.unis_rssol.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private final AppUserRepository users;

    // ⭐️ S3 기본 이미지 URL (UserService에서 이동)
    private static final String STAFF_DEFAULT_URL = "https://rssol-bucket.s3.ap-northeast-2.amazonaws.com/staff.png";
    private static final String OWNER_DEFAULT_URL = "https://rssol-bucket.s3.ap-northeast-2.amazonaws.com/owner.png";
    

    /**
     * 역할(OWNER/STAFF) 선택에 따라 기본 프로필 이미지를 업데이트합니다.
     * (사용자가 커스텀 이미지를 사용하는 경우는 건드리지 않습니다)
     */
    public void updateDefaultImageForRole(AppUser u, String newRole) {

        String currentImageUrl = u.getProfileImageUrl();

        // 현재 이미지가 '기본 staff' 이미지이고, 선택한 역할이 'OWNER'일 경우
        if (newRole.equals("OWNER") && STAFF_DEFAULT_URL.equals(currentImageUrl)) {
            u.setProfileImageUrl(OWNER_DEFAULT_URL);
            users.save(u); // AppUser 변경 사항 저장
            log.info("User {} selected OWNER, updating profile image to default owner image.", u.getId());
        }
        // (반대 경우) 현재 이미지가 '기본 owner' 이미지이고, 선택한 역할이 'STAFF'일 경우
        else if (newRole.equals("STAFF") && OWNER_DEFAULT_URL.equals(currentImageUrl)) {
            u.setProfileImageUrl(STAFF_DEFAULT_URL);
            users.save(u); // AppUser 변경 사항 저장
            log.info("User {} selected STAFF, updating profile image to default staff image.", u.getId());
        }
        // (그 외의 경우, 즉 사용자가 카카오 커스텀 이미지를 쓰고 있는 경우는 아무것도 안 함)

        // (참고: @Transactional 환경이라 save()를 생략하고 'dirty checking'을 활용할 수도 있습니다.)
    }
}
