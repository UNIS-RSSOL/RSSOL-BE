package com.example.unis_rssol.schedule.staffing.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class StaffingRequestDetailDto {
    private Long requestId;
    private Long storeId;
    private Long ownerUserId;
    private Long baseShiftId;
    private String start;
    private String end;
    private int headcountRequested;
    private int headcountFilled;
    private String status;
    private String note;
    private List<Long> receiverUserIds;
}
