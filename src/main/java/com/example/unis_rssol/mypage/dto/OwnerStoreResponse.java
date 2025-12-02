package com.example.unis_rssol.mypage.dto;

import lombok.*;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class OwnerStoreResponse {
    private Long storeId;
    private String storeCode;                    // 읽기 전용
    private String name;
    private String address;
    private String phoneNumber;

}
