package com.example.unis_rssol.domain.notification.dto;

import com.example.unis_rssol.domain.schedule.extrashift.entity.ExtrashiftRequest;
import com.example.unis_rssol.domain.schedule.shiftswap.ShiftSwapRequest;
import com.example.unis_rssol.domain.notification.Notification;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponseDto {

    private String storeName;
    private String profileImageUrl;

    private Notification.Category category;
    private Notification.Type type;
    private String message;

    private LocalDateTime createdAt;

    // ===== 요청 ID =====
    private Long shiftSwapRequestId;
    private Long extraShiftRequestId;

    // ===== 상태 (프론트 버튼 제어용) =====
    private ShiftSwapRequest.Status shiftSwapStatus;
    private ShiftSwapRequest.ManagerApproval shiftSwapManagerApprovalStatus;
    private ExtrashiftRequest.Status extraShiftStatus;

    private boolean isRead;
}
