package com.example.unis_rssol.user.controller;

import com.example.unis_rssol.user.dto.SelectRoleRequest;
import com.example.unis_rssol.user.dto.SelectRoleResponse;
import com.example.unis_rssol.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/onboarding")
@RequiredArgsConstructor
public class UserOnboardingController {

    private final UserService userService;

    // 역할 선택 (OWNER / STAFF)
    @PostMapping("/role")
    public ResponseEntity<SelectRoleResponse> role(
            @AuthenticationPrincipal Long userId,
            @RequestBody SelectRoleRequest req
    ) {
        return ResponseEntity.ok(userService.setRole(userId, req));
    }
}
