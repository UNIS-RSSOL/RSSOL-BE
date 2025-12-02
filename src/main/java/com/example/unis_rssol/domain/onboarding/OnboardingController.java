package com.example.unis_rssol.domain.onboarding;

import com.example.unis_rssol.domain.onboarding.dto.OnboardingRequest;
import com.example.unis_rssol.domain.onboarding.dto.OnboardingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;
// 온보딩 - 역할 + 매장(생성/참여) + 계좌 등록까지 한 번에
    @PostMapping
    public ResponseEntity<OnboardingResponse> onboarding(
            @AuthenticationPrincipal Long userId,
            @RequestBody OnboardingRequest req
    ) {
        return ResponseEntity.ok(onboardingService.onboard(userId, req));
    }
}
