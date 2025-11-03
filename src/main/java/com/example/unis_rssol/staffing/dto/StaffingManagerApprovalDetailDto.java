package com.example.unis_rssol.staffing.dto;

import com.example.unis_rssol.staffing.entity.StaffingRequest;
import com.example.unis_rssol.staffing.entity.StaffingResponse;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StaffingManagerApprovalDetailDto {

    private Long requestId;
    private Long responseId;

    private Long storeId;
    private Long ownerUserId;
    private Long candidateUserId;

    private String start;
    private String end;

    private Integer headcountRequested;
    private Integer headcountFilled;
    private String requestStatus;       // OPEN/FILLED/...

    private String workerAction;        // ACCEPT/REJECT/...
    private String managerApproval;     // APPROVED/REJECTED/PENDING

    private boolean shiftAssigned;      // 승인 시 새 WorkShift 생성 여부

    public static StaffingManagerApprovalDetailDto of(StaffingRequest req, StaffingResponse resp, boolean shiftAssigned) {
        return StaffingManagerApprovalDetailDto.builder()
                .requestId(req.getId())
                .responseId(resp.getId())
                .storeId(req.getStore().getId())
                .ownerUserId(req.getOwner().getUser().getId())
                .candidateUserId(resp.getCandidate().getUser().getId())
                .start(req.getStartDatetime().toString())
                .end(req.getEndDatetime().toString())
                .headcountRequested(req.getHeadcountRequested())
                .headcountFilled(req.getHeadcountFilled())
                .requestStatus(req.getStatus().name())
                .workerAction(resp.getWorkerAction().name())
                .managerApproval(resp.getManagerApproval().name())
                .shiftAssigned(shiftAssigned)
                .build();
    }
}
