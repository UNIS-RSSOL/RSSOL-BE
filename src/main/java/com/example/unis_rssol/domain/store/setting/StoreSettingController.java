package com.example.unis_rssol.domain.store.setting;

import com.example.unis_rssol.domain.store.UserStore;
import com.example.unis_rssol.global.security.AuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 매장 설정 API
 *
 * - 기본 설정 조회/수정
 * - 임시 설정 저장/조회/적용 (스케줄 생성 시 사용)
 */
@RestController
@RequestMapping("/api/store-settings")
@RequiredArgsConstructor
public class StoreSettingController {

    private final StoreSettingService storeSettingService;
    private final AuthorizationService authService;

    /**
     * 현재 활성화된 매장의 설정 조회
     * GET /api/store-settings
     */
    @GetMapping
    public ResponseEntity<StoreSettingDto> getStoreSetting(@AuthenticationPrincipal Long userId) {
        Long storeId = authService.getActiveStoreIdOrThrow(userId);
        StoreSetting setting = storeSettingService.getStoreSetting(storeId);
        return ResponseEntity.ok(storeSettingService.toDto(setting));
    }

    /**
     * 매장 설정 업데이트 (OWNER 전용)
     * PATCH /api/store-settings
     */
    @PatchMapping
    public ResponseEntity<StoreSettingDto> updateStoreSetting(
            @AuthenticationPrincipal Long userId,
            @RequestBody StoreSettingDto dto
    ) {
        Long storeId = authService.getActiveStoreIdOrThrow(userId);
        UserStore userStore = authService.getUserStoreOrThrow(userId, storeId);

        if (userStore.getPosition() != UserStore.Position.OWNER) {
            return ResponseEntity.status(403).build();
        }

        StoreSetting updated = storeSettingService.updateStoreSetting(storeId, dto);
        return ResponseEntity.ok(storeSettingService.toDto(updated));
    }

    /**
     * 임시 설정 저장 (Redis)
     * POST /api/store-settings/temporary
     *
     * 스케줄 생성 시 설정을 임시로 수정할 때 사용
     * 반환: Redis key
     */
    @PostMapping("/temporary")
    public ResponseEntity<Map<String, String>> saveTemporarySetting(
            @AuthenticationPrincipal Long userId,
            @RequestBody StoreSettingDto dto
    ) {
        Long storeId = authService.getActiveStoreIdOrThrow(userId);
        UserStore userStore = authService.getUserStoreOrThrow(userId, storeId);

        if (userStore.getPosition() != UserStore.Position.OWNER) {
            return ResponseEntity.status(403).build();
        }

        String redisKey = storeSettingService.saveTemporarySetting(storeId, dto);
        return ResponseEntity.ok(Map.of("temporaryKey", redisKey));
    }

    /**
     * 임시 설정 조회 (Redis)
     * GET /api/store-settings/temporary?key={redisKey}
     */
    @GetMapping("/temporary")
    public ResponseEntity<StoreSettingDto> getTemporarySetting(@RequestParam String key) {
        StoreSettingDto dto = storeSettingService.getTemporarySetting(key);
        return ResponseEntity.ok(dto);
    }

    /**
     * 임시 설정을 기본 설정으로 적용 (OWNER 전용)
     * POST /api/store-settings/temporary/apply?key={redisKey}
     */
    @PostMapping("/temporary/apply")
    public ResponseEntity<StoreSettingDto> applyTemporaryAsDefault(
            @AuthenticationPrincipal Long userId,
            @RequestParam String key
    ) {
        Long storeId = authService.getActiveStoreIdOrThrow(userId);
        UserStore userStore = authService.getUserStoreOrThrow(userId, storeId);

        if (userStore.getPosition() != UserStore.Position.OWNER) {
            return ResponseEntity.status(403).build();
        }

        StoreSetting applied = storeSettingService.applyTemporarySettingAsDefault(storeId, key);
        return ResponseEntity.ok(storeSettingService.toDto(applied));
    }

    /**
     * 임시 설정 삭제
     * DELETE /api/store-settings/temporary?key={redisKey}
     */
    @DeleteMapping("/temporary")
    public ResponseEntity<Void> deleteTemporarySetting(@RequestParam String key) {
        storeSettingService.deleteTemporarySetting(key);
        return ResponseEntity.noContent().build();
    }
}

