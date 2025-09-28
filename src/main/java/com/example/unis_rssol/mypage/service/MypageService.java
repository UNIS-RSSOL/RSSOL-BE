package com.example.unis_rssol.mypage.service;

import com.example.unis_rssol.mypage.dto.*;

import java.util.List;

public interface MypageService {

    // 활성 매장 선택 관련 - 인스타처럼 여러 매장 리스트 중 일정 매장을 선택
    ActiveStoreResponse getActiveStore(Long userId);
    ActiveStoreResponse updateActiveStore(Long userId, Long storeId);

    // 사장님 관련
    OwnerProfileResponse getOwnerProfile(Long ownerId);
    OwnerProfileResponse updateOwnerProfile(Long ownerId, OwnerProfileUpdateRequest request);

    OwnerStoreResponse getOwnerActiveStore(Long ownerId);
    OwnerStoreResponse updateOwnerActiveStore(Long ownerId, OwnerStoreUpdateRequest request);

    List<StoreSimpleResponse> listOwnerStores(Long ownerId);
    StoreSimpleResponse addOwnerStore(Long ownerId, OwnerCreateStoreRequest request);
    void removeOwnerStore(Long ownerId, Long storeId);

    //  알바생
    StaffProfileResponse getStaffProfile(Long staffId);
    StaffProfileResponse updateStaffProfile(Long staffId, StaffProfileUpdateRequest request);

    List<StoreSimpleResponse> listStaffStores(Long staffId);
    StoreSimpleResponse joinStaffStore(Long staffId, StaffJoinStoreRequest request);
    void leaveStaffStore(Long staffId, Long storeId);
}
