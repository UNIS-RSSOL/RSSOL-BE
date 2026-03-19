package com.rssolplan.api.domain.payroll.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 급여 지급 상태 DTO
 * - Owner가 직원에게 급여를 지급했는지 조회/수정
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRecordDto {

    private Long paymentRecordId;       // 지급 기록 ID
    private Long userStoreId;           // UserStore ID
    private Long userId;                // User ID
    private String username;            // 직원 이름
    private Long storeId;               // 매장 ID
    private String storeName;           // 매장 이름
    private int year;                   // 년도
    private int month;                  // 월
    private boolean isPaid;             // 지급 여부
    private LocalDateTime paidAt;       // 지급 시간
    private Long paidByUserId;          // 지급 처리자 ID
}

