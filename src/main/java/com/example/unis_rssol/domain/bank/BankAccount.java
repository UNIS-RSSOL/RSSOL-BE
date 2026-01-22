package com.example.unis_rssol.domain.bank;

import com.example.unis_rssol.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="bank_accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BankAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id", nullable=false)
    private User user;

    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="bank_id", nullable=false)
    private Bank bank;

    private String accountNumber;
}