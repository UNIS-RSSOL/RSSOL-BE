package com.rssolplan.api.domain.mypage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @AllArgsConstructor @NoArgsConstructor
public class OwnerStoreUpdateRequest {
    private String name;
    private String address;
    private String phoneNumber;
}
