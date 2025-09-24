package com.example.unis_rssol.bank.service;

import com.example.unis_rssol.bank.dto.*;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class StaffOnboardingService {
    private final AppUserRepository users;
    private final StoreRepository stores;
    private final UserStoreRepository userStores;
    private final BankRepository banks;
    private final BankAccountRepository accounts;

    @Transactional
    public StaffJoinResponse join(Long userId, StaffJoinRequest req){
        AppUser staff = users.findById(userId).orElseThrow();
        Store store = stores.findByStoreCode(req.getStoreCode()).orElseThrow(() -> new IllegalArgumentException("Invalid store code"));
        Bank bank = banks.findById(req.getBankId()).orElseThrow();

        // 매장-사용자 관계(알바) 생성 (중복 UNIQUE 제약에 의해 1회만)
        UserStore link = userStores.save(UserStore.builder()
                .user(staff).store(store)
                .position(UserStore.Position.STAFF)
                .build());

        // 계좌 저장
        BankAccount account = accounts.save(BankAccount.builder()
                .user(staff).bank(bank)
                .accountNumber(req.getAccountNumber())
                .build());

        return new StaffJoinResponse(
                staff.getId(), link.getId(), store.getId(),
                "STAFF", "HIRED",
                store.getName(), store.getAddress(), store.getPhoneNumber(),
                bank.getId(), bank.getBankName(), account.getAccountNumber()
        );
    }
}