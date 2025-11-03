package com.example.unis_rssol.staffing.dto;

import com.example.unis_rssol.staffing.entity.StaffingRequest;
import com.example.unis_rssol.staffing.entity.StaffingResponse;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StaffingResponseDetailDto {

    private Long requestId;
    private Long responseId;

    private Long storeId;
    private Long ownerUserId;
    private Long candidateUserId;

    private String start; // ISO
    private String end;   // ISO

    private Integer headcountRequested;
    private Integer headcountFilled;

    private String requestStatus;      // StaffingRequest.Status
    private String workerAction;       // StaffingResponse.WorkerAction
    private String managerApproval;    // StaffingResponse.ManagerApproval

    private String createdAt; // response.createdAt (ISO)

    public static StaffingResponseDetailDto of(StaffingRequest req, StaffingResponse resp) {
        return StaffingResponseDetailDto.builder()
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
                .createdAt(resp.getCreatedAt().toString())
                .build();
    }
}
