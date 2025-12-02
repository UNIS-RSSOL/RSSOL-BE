package com.example.unis_rssol.domain.bank.entity;

import com.example.unis_rssol.domain.user.entity.AppUser;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="bank_accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BankAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id", nullable=false)
    private AppUser user;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="bank_id", nullable=false)
    private Bank bank;

    private String accountNumber;
}