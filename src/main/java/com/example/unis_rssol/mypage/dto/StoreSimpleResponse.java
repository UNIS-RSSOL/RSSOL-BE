package com.example.unis_rssol.mypage.dto;

import lombok.*;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class StoreSimpleResponse {
    private Long storeId;
    private String storeCode;
    private String name;
    private String address;
    private String phoneNumber;
    private String businessRegistrationNumber;

    // 목록/활성 조회 시에는 보여주고,
    // 등록 응답에서는 null로 둘 수 있음 ㅇㅇ (등록 응답에서 employmentStatus 제외)
    private String position;           // OWNER or STAFF
    private String employmentStatus;   // HIRED | ON_LEAVE | RESIGNED
}
