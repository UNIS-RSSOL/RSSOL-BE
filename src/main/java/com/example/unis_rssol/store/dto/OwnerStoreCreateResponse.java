package com.example.unis_rssol.store.dto;

import lombok.Getter; import lombok.AllArgsConstructor;
@Getter @AllArgsConstructor
public class OwnerStoreCreateResponse {
    private Long userId;
    private Long userStoreId;
    private Long storeId;
    private String position;  // OWNER
    private String employmentStatus;
    private String storeCode;
    private String name; private String address; private String phoneNumber; private String businessRegistrationNumber;
}