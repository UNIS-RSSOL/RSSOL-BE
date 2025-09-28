package com.example.unis_rssol.store.dto;

import lombok.Getter; import lombok.AllArgsConstructor;

@Getter
public class OwnerStoreCreateRequest {
    private String name;
    private String address;
    private String phoneNumber;
    private String businessRegistrationNumber;
}
