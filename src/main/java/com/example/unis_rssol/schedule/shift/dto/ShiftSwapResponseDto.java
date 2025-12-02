package com.example.unis_rssol.schedule.shift.dto;

import com.example.unis_rssol.schedule.shift.entity.ShiftSwapRequest;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ShiftSwapResponseDto {
    private Long requestId;
    private Long shiftId;
    private Long requesterId;
    private Long receiverId;
    private String reason;
    private ShiftSwapRequest.Status status;
    private ShiftSwapRequest.ManagerApproval managerApprovalStatus;
    private LocalDateTime createdAt;

    public static ShiftSwapResponseDto from(ShiftSwapRequest e) {
        return ShiftSwapResponseDto.builder()
                .requestId(e.getId())
                .shiftId(e.getShift().getId())
                .requesterId(e.getRequester().getId())
                .receiverId(e.getReceiver().getId())
                .reason(e.getReason())
                .status(e.getStatus())
                .managerApprovalStatus(e.getManagerApprovalStatus())
                .createdAt(e.getCreatedAt())
                .build();
    }
}