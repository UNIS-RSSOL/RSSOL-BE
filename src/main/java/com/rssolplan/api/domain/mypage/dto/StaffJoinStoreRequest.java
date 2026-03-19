package com.rssolplan.api.domain.mypage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class StaffJoinStoreRequest {
    private String storeCode;
    private LocalDate hireDate;
}
