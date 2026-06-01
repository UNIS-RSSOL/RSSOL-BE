package com.rssolplan.api.global.security.email_verification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 이메일 인증 응답 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailVerificationResponse {

    private boolean success;
    private String message;
}

