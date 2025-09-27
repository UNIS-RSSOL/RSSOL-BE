package com.example.unis_rssol.mypage.controller;

import com.example.unis_rssol.mypage.dto.*;
import com.example.unis_rssol.mypage.service.MypageService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mypage")
public class MypageController {

    private final MypageService service;

    // 직접 생성자 정의
    public MypageController(MypageService service) {
        this.service = service;
    }

    // ===== 활성 매장 =====

    @GetMapping("/active-store")
    public ResponseEntity<ActiveStoreResponse> getActiveStore(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(service.getActiveStore(userId));
    }

    @PatchMapping("/active-store/{storeId}")
    public ResponseEntity<ActiveStoreResponse> updateActiveStore(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long storeId) {
        return ResponseEntity.ok(service.updateActiveStore(userId, storeId));
    }

    // ===== 사장님 =====

    @GetMapping("/owner/profile")
    public ResponseEntity<OwnerProfileResponse> getOwnerProfile(@AuthenticationPrincipal Long ownerId) {
        return ResponseEntity.ok(service.getOwnerProfile(ownerId));
    }

    @PutMapping("/owner/profile")
    public ResponseEntity<OwnerProfileResponse> updateOwnerProfile(
            @AuthenticationPrincipal Long ownerId,
            @RequestBody OwnerProfileUpdateRequest request) {
        return ResponseEntity.ok(service.updateOwnerProfile(ownerId, request));
    }

    @GetMapping("/owner/store")
    public ResponseEntity<OwnerStoreResponse> getOwnerActiveStore(@AuthenticationPrincipal Long ownerId) {
        return ResponseEntity.ok(service.getOwnerActiveStore(ownerId));
    }

    @PutMapping("/owner/store")
    public ResponseEntity<OwnerStoreResponse> updateOwnerActiveStore(
            @AuthenticationPrincipal Long ownerId,
            @RequestBody OwnerStoreUpdateRequest request) {
        return ResponseEntity.ok(service.updateOwnerActiveStore(ownerId, request));
    }

    @GetMapping("/owner/stores")
    public ResponseEntity<List<StoreSimpleResponse>> listOwnerStores(@AuthenticationPrincipal Long ownerId) {
        return ResponseEntity.ok(service.listOwnerStores(ownerId));
    }

    @PostMapping("/owner/stores")
    public ResponseEntity<StoreSimpleResponse> addOwnerStore(
            @AuthenticationPrincipal Long ownerId,
            @RequestBody OwnerCreateStoreRequest request) {
        return ResponseEntity.status(201).body(service.addOwnerStore(ownerId, request));
    }

    @DeleteMapping("/owner/stores/{storeId}")
    public ResponseEntity<Void> removeOwnerStore(
            @AuthenticationPrincipal Long ownerId,
            @PathVariable Long storeId) {
        service.removeOwnerStore(ownerId, storeId);
        return ResponseEntity.noContent().build();
    }

    // ===== 알바생 =====

    @GetMapping("/staff/profile")
    public ResponseEntity<StaffProfileResponse> getStaffProfile(@AuthenticationPrincipal Long staffId) {
        return ResponseEntity.ok(service.getStaffProfile(staffId));
    }

    @PutMapping("/staff/profile")
    public ResponseEntity<StaffProfileResponse> updateStaffProfile(
            @AuthenticationPrincipal Long staffId,
            @RequestBody StaffProfileUpdateRequest request) {
        return ResponseEntity.ok(service.updateStaffProfile(staffId, request));
    }

    @GetMapping("/staff/stores")
    public ResponseEntity<List<StoreSimpleResponse>> listStaffStores(@AuthenticationPrincipal Long staffId) {
        return ResponseEntity.ok(service.listStaffStores(staffId));
    }

    @PostMapping("/staff/stores")
    public ResponseEntity<StoreSimpleResponse> joinStaffStore(
            @AuthenticationPrincipal Long staffId,
            @RequestBody StaffJoinStoreRequest request) {
        return ResponseEntity.status(201).body(service.joinStaffStore(staffId, request));
    }

    @DeleteMapping("/staff/stores/{storeId}")
    public ResponseEntity<Void> leaveStaffStore(
            @AuthenticationPrincipal Long staffId,
            @PathVariable Long storeId) {
        service.leaveStaffStore(staffId, storeId);
        return ResponseEntity.noContent().build();
    }
}
