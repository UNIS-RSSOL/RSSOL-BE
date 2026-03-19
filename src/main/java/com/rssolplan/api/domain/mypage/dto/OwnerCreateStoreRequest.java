package com.rssolplan.api.domain.mypage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class OwnerCreateStoreRequest {
    private String name;
    private String address;
    private String phoneNumber;
    private String businessRegistrationNumber;
    private LocalDate hireDate;

}
