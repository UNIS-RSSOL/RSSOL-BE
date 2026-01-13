package com.example.unis_rssol.mypage.dto;

import lombok.*;

import java.time.LocalDate;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class OwnerCreateStoreRequest {
    private String name;
    private String address;
    private String phoneNumber;
    private String businessRegistrationNumber;
    private LocalDate hireDate;

}
