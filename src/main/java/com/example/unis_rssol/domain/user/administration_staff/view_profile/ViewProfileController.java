package com.example.unis_rssol.domain.user.administration_staff.view_profile;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/administration-staff/employees")
@RequiredArgsConstructor
public class ViewProfileController {

    private final ViewProfileService viewProfileService;

    @GetMapping("/{userStoreId}/profile")
    public ViewProfileResponse getEmployeeProfile(
            @AuthenticationPrincipal Long ownerId,
            @PathVariable Long userStoreId
    ) {
        return viewProfileService.getEmployeeProfile(
                ownerId,
                userStoreId
        );
    }
}