package com.example.unis_rssol.mypage.dto;

import lombok.*;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class OwnerStoreUpdateRequest {
    private String name;
    private String address;
    private String phoneNumber;
}
