package com.example.unis_rssol.domain.bank.repository;

import com.example.unis_rssol.domain.bank.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {
    //  가장 최근 계좌(대표 계좌처럼 사용)
    Optional<BankAccount> findTopByUserIdOrderByIdDesc(Long userId);
}
