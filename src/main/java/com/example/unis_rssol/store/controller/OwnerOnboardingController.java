package com.example.unis_rssol.store.controller;

import com.example.unis_rssol.store.dto.*;
import com.example.unis_rssol.store.service.OwnerOnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/onboarding/owner")
@RequiredArgsConstructor
public class OwnerOnboardingController {
    private final OwnerOnboardingService service;

    @PreAuthorize("hasRole('OWNER')")
    @PostMapping("/store")
    public ResponseEntity<OwnerStoreCreateResponse> register(
            @AuthenticationPrincipal Long userId,
            @RequestBody OwnerStoreCreateRequest req
    ) {
        return ResponseEntity.ok(service.createStore(userId, req));
    }
}
