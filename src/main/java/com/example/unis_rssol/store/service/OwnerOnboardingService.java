package com.example.unis_rssol.store.service;

import com.example.unis_rssol.global.StoreCodeGenerator;
import com.example.unis_rssol.store.dto.*;
import com.example.unis_rssol.store.entity.Store;
import com.example.unis_rssol.store.entity.UserStore;
import com.example.unis_rssol.store.repository.StoreRepository;
import com.example.unis_rssol.store.repository.UserStoreRepository;
import com.example.unis_rssol.user.entity.AppUser;
import com.example.unis_rssol.user.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class OwnerOnboardingService {
    private final AppUserRepository users;
    private final StoreRepository stores;
    private final UserStoreRepository userStores;

    @Transactional
    public OwnerStoreCreateResponse createStore(Long userId, OwnerStoreCreateRequest req){
        AppUser owner = users.findById(userId).orElseThrow();

        Store store = Store.builder()
                .storeCode(StoreCodeGenerator.generate())
                .name(req.getName())
                .address(req.getAddress())
                .phoneNumber(req.getPhoneNumber())
                .businessRegistrationNumber(req.getBusinessRegistrationNumber())
                .build();
        stores.save(store);

        UserStore link = UserStore.builder()
                .user(owner).store(store)
                .position(UserStore.Position.OWNER)
                .build();
        userStores.save(link);

        return new OwnerStoreCreateResponse(owner.getId(), link.getId(), store.getId(),
                "OWNER", "HIRED", store.getStoreCode(),
                store.getName(), store.getAddress(), store.getPhoneNumber(), store.getBusinessRegistrationNumber());
    }
}