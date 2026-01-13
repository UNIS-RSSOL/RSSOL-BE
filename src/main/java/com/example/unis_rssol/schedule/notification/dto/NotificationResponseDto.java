package com.example.unis_rssol.schedule.notification.dto;


import com.example.unis_rssol.schedule.notification.Notification;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class NotificationResponseDto {

    private String storeName;
    private String profileImageUrl;

    private Notification.Category category; // SHIFT_SWAP | EXTRA_SHIFT | SCHEDULE_INPUT_REQUEST
    private Notification.Type type;
    private String message;

    private LocalDateTime createdAt;

    // 대타 요청일 경우 사용
    private Long shiftSwapRequestId;

    // 추가 인력 요청일 경우 사용
    private Long extraShiftRequestId;

    private boolean isRead;

}

