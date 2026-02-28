package com.example.unis_rssol.domain.payroll.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AllStaffWageResponseDto {
    private String username;
    private String phone;
    private String bankName; //은행이름
    private String bankAccount; //은행계좌
    private int late; //지각
    private int miss; // 결근
}
