package com.example.unis_rssol.bank.controller;

import com.example.unis_rssol.bank.dto.*;
import com.example.unis_rssol.bank.service.StaffOnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/onboarding/staff")
@RequiredArgsConstructor
public class StaffOnboardingController {
    private final StaffOnboardingService service;

    @PreAuthorize("hasRole('STAFF')")
    @PostMapping
    public ResponseEntity<StaffJoinResponse> staff(
            @AuthenticationPrincipal Long userId,
            @RequestBody StaffJoinRequest req
    ) {
        return ResponseEntity.ok(service.join(userId, req));
    }
}
