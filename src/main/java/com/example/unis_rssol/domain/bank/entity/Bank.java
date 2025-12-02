package com.example.unis_rssol.domain.bank.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="bank")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Bank {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Integer id;
    private String bankName; private String bankCode;
}