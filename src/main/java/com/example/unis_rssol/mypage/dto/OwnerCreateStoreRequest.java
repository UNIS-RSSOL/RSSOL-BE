package com.example.unis_rssol.mypage.dto;

import lombok.*;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class OwnerCreateStoreRequest {
    private String name;
    private String address;
    private String phoneNumber;
    private String businessRegistrationNumber;
}
