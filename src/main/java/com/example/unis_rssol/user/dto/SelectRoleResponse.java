package com.example.unis_rssol.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter @AllArgsConstructor
public class SelectRoleResponse {
    private Long userId;
    private String position;
    private boolean isNewUser;
}
