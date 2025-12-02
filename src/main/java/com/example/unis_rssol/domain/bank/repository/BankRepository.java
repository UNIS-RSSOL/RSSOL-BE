package com.example.unis_rssol.domain.bank.repository;

import com.example.unis_rssol.domain.bank.entity.Bank;
import org.springframework.data.jpa.repository.JpaRepository;
public interface BankRepository extends JpaRepository<Bank, Integer> {}