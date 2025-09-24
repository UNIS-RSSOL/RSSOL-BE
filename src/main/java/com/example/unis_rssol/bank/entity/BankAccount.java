package com.example.unis_rssol.bank.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="bank_accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BankAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id", nullable=false)
    private com.example.unis_rssol.user.entity.AppUser user;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="bank_id", nullable=false)
    private Bank bank;

    private String accountNumber;
}