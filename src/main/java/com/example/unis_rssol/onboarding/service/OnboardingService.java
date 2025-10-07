package com.example.unis_rssol.onboarding.service;

import com.example.unis_rssol.onboarding.dto.OnboardingRequest;
import com.example.unis_rssol.onboarding.dto.OnboardingResponse;
import com.example.unis_rssol.bank.entity.Bank;
import com.example.unis_rssol.bank.entity.BankAccount;
import com.example.unis_rssol.bank.repository.BankAccountRepository;
import com.example.unis_rssol.bank.repository.BankRepository;
import com.example.unis_rssol.store.entity.Store;
import com.example.unis_rssol.store.entity.UserStore;
import com.example.unis_rssol.store.repository.StoreRepository;
import com.example.unis_rssol.store.repository.UserStoreRepository;
import com.example.unis_rssol.user.entity.AppUser;
import com.example.unis_rssol.user.repository.AppUserRepository;
import com.example.unis_rssol.global.util.StoreCodeGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final AppUserRepository users;
    private final StoreRepository stores;
    private final UserStoreRepository userStores;
    private final BankRepository banks;
    private final BankAccountRepository accounts;

    @Transactional
    public OnboardingResponse onboard(Long userId, OnboardingRequest req) {
        AppUser user = users.findById(userId).orElseThrow();

        Store store;
        if ("OWNER".equalsIgnoreCase(req.getRole())) {
            // 사장: 매장 새로 생성
            store = Store.builder()
                    .storeCode(StoreCodeGenerator.generate())
                    .name(req.getName())
                    .address(req.getAddress())
                    .phoneNumber(req.getPhoneNumber())
                    .businessRegistrationNumber(req.getBusinessRegistrationNumber())
                    .build();
            stores.save(store);

        } else if ("STAFF".equalsIgnoreCase(req.getRole())) {
            // 알바: 매장 코드로 참여
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
                .hireDate("STAFF".equalsIgnoreCase(req.getRole()) ? req.getHireDate() : null)
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
            account = accounts.save(BankAccount.builder()
                    .user(user)
                    .bank(bank)
                    .accountNumber(req.getAccountNumber())
                    .build());
        }

        return new OnboardingResponse(
                user.getId(), link.getId(), store.getId(),
                req.getRole().toUpperCase(), "HIRED",
                store.getStoreCode(), store.getName(),
                store.getAddress(), store.getPhoneNumber(),
                store.getBusinessRegistrationNumber(),
                bank != null ? bank.getId() : null,
                bank != null ? bank.getBankName() : null,
                account != null ? account.getAccountNumber() : null,
                link.getHireDate()
        );
    }
}
