package com.example.unis_rssol.store;

import com.example.unis_rssol.auth.dto.LoginResponse;
import com.example.unis_rssol.auth.service.AuthService;
import com.example.unis_rssol.global.auth.AuthorizationService;
import com.example.unis_rssol.store.entity.UserStore;
import com.example.unis_rssol.store.repository.UserStoreRepository;
import com.example.unis_rssol.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/store")
@RequiredArgsConstructor
public class StoreController {

    private final AuthorizationService authService;
    private final UserStoreRepository userStoreRepository;

    @GetMapping("/staff")
    public ResponseEntity<List<StoreStaffResponse>> getAllStaff(@AuthenticationPrincipal Long userId) {
        Long storeId = authService.getActiveStoreIdOrThrow(userId);

        // 2. 매장에 속한 모든 UserStore 가져오기
        List<UserStore> staffList = userStoreRepository.findByStore_Id(storeId);

        List<StoreStaffResponse> response = staffList.stream()
                .map(us -> new StoreStaffResponse(
                        us.getId(),               // userStoreId
                        us.getUser().getUsername() // username
                ))
                .toList();

        return ResponseEntity.ok(response);
    }
}
