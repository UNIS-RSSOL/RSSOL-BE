package com.rssolplan.api.domain.store;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class StoreStaffResponse {
    private Long userStoreId;
    private String username;
}