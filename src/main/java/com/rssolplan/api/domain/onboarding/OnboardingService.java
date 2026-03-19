package com.rssolplan.api.domain.onboarding;

import com.rssolplan.api.domain.bank.Bank;
import com.rssolplan.api.domain.bank.BankAccount;
import com.rssolplan.api.domain.bank.BankAccountRepository;
import com.rssolplan.api.domain.bank.BankRepository;
import com.rssolplan.api.domain.onboarding.dto.OnboardingRequest;
import com.rssolplan.api.domain.onboarding.dto.OnboardingResponse;
import com.rssolplan.api.domain.store.Store;
import com.rssolplan.api.domain.store.StoreRepository;
import com.rssolplan.api.domain.store.UserStore;
import com.rssolplan.api.domain.store.UserStoreRepository;
import com.rssolplan.api.domain.store.setting.StoreSettingDto;
import com.rssolplan.api.domain.store.setting.StoreSettingService;
import com.rssolplan.api.domain.user.User;
import com.rssolplan.api.domain.user.UserProfileService;
import com.rssolplan.api.domain.user.UserRepository;
import com.rssolplan.api.global.fordevToken.StoreCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final UserRepository users;
    private final StoreRepository stores;
    private final UserStoreRepository userStores;
    private final BankRepository banks;
    private final BankAccountRepository accounts;
    private final UserProfileService userProfileService;
    private final StoreSettingService storeSettingService;

    @Transactional
    public OnboardingResponse onboard(Long userId, OnboardingRequest req) {

        User user = users.findById(userId).orElseThrow();

        Store store;
        if ("OWNER".equalsIgnoreCase(req.getRole())) {

            store = Store.builder()
                    .storeCode(StoreCodeGenerator.generate())
                    .name(req.getName())
                    .address(req.getAddress())
                    .phoneNumber(req.getPhoneNumber())
                    .businessRegistrationNumber(req.getBusinessRegistrationNumber())
                    .build();
            stores.save(store);

            // OWNER일 경우 StoreSetting 저장
            if (req.getStoreSetting() != null) {
                storeSettingService.createStoreSetting(store, req.getStoreSetting());
            }

        } else if ("STAFF".equalsIgnoreCase(req.getRole())) {

            store = stores.findByStoreCode(req.getStoreCode())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid store code"));

        } else {
            throw new IllegalArgumentException("Invalid role: " + req.getRole());
        }

        UserStore link = UserStore.builder()
                .user(user)
                .store(store)
                .position(UserStore.Position.valueOf(req.getRole().toUpperCase()))
                .employmentStatus(UserStore.EmploymentStatus.HIRED)
                .hireDate(req.getHireDate())
                .build();

        userStores.save(link);

        // 활성 매장 설정
        user.setActiveStoreId(store.getId());
        users.save(user);

        // 계좌 저장 (선택)
        Bank bank = null;
        BankAccount account = null;
        if (req.getBankId() != null && req.getAccountNumber() != null) {
            bank = banks.findById(req.getBankId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid bank id"));
            account = accounts.save(
                    BankAccount.builder()
                            .user(user)
                            .bank(bank)
                            .accountNumber(req.getAccountNumber())
                            .build()
            );
        }

        userProfileService.updateDefaultImageForRole(user, req.getRole().toUpperCase());

        // StoreSetting을 Response에 포함
        StoreSettingDto storeSettingDto;
        if ("OWNER".equalsIgnoreCase(req.getRole()) && req.getStoreSetting() != null) {
            // 방금 생성한 설정을 그대로 반환
            storeSettingDto = req.getStoreSetting();
        } else {
            // 기존 매장 설정이 있다면 조회
            storeSettingDto = storeSettingService.findStoreSetting(store.getId())
                    .map(storeSettingService::toDto)
                    .orElse(null);
        }

        return new OnboardingResponse(
                user.getId(),
                link.getId(),
                store.getId(),
                req.getRole().toUpperCase(),
                "HIRED",
                store.getStoreCode(),
                store.getName(),
                store.getAddress(),
                store.getPhoneNumber(),
                store.getBusinessRegistrationNumber(),
                bank != null ? bank.getId() : null,
                bank != null ? bank.getBankName() : null,
                account != null ? account.getAccountNumber() : null,
                link.getHireDate(),
                storeSettingDto
        );
    }
}
