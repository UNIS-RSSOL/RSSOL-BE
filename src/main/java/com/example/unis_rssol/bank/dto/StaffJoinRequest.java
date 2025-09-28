package com.example.unis_rssol.bank.dto;

import lombok.Getter; import lombok.AllArgsConstructor;

@Getter
public class StaffJoinRequest {
    private String storeCode;
    private Integer bankId;
    private String accountNumber;
}
