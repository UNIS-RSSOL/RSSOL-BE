package com.example.unis_rssol.bank.repository;

import com.example.unis_rssol.bank.entity.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {}