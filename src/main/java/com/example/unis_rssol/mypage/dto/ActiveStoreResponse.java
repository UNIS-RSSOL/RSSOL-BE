package com.example.unis_rssol.mypage.dto;

import lombok.*;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class ActiveStoreResponse {
    private Long storeId;
    private String storeCode;
    private String name; // 가게 이름
    private String address;
    private String phoneNumber;
    private String businessRegistrationNumber;
    private String position; // 해당 가게를 활성화시킨 주체의 포지션 - 알바생인지 사장님인지 받음
    private String employmentStatus;
}
