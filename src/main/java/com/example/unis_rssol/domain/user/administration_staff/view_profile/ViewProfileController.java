package com.example.unis_rssol.domain.user.administration_staff.view_profile;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/administration-staff/employees")
@RequiredArgsConstructor
public class ViewProfileController {

    private final ViewProfileService viewProfileService;

    // 직원 프로필 조회
    @GetMapping("/{userStoreId}/profile")
    public ViewProfileResponse getEmployeeProfile(
            @PathVariable Long userStoreId
    ) {
        return viewProfileService.getEmployeeProfile(userStoreId);
    }
}
