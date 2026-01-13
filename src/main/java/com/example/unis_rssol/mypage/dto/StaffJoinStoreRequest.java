package com.example.unis_rssol.mypage.dto;

import lombok.*;

import java.time.LocalDate;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class StaffJoinStoreRequest {
    private String storeCode;
    private LocalDate hireDate;
}
