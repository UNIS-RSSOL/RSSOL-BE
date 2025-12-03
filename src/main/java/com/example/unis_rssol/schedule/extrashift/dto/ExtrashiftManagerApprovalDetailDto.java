package com.example.unis_rssol.schedule.extrashift.dto;

import com.example.unis_rssol.schedule.extrashift.entity.ExtrashiftRequest;
import com.example.unis_rssol.schedule.extrashift.entity.ExtrashiftResponse;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExtrashiftManagerApprovalDetailDto {

    private Long requestId;
    private Long responseId;

    private Long storeId;
    private Long ownerUserId;
    private Long candidateUserId;

    private String start;
    private String end;

    private Integer headcountRequested;
    private Integer headcountFilled;
    private String requestStatus;

    private String workerAction;        // ACCEPT/REJECT/...
    private String managerApproval;     // APPROVED/REJECTED/PENDING

    private boolean shiftAssigned;      // 승인 시 새 WorkShift 생성 여부 - 추가 인력에 대한 work_shift 행 추가

    public static ExtrashiftManagerApprovalDetailDto of(ExtrashiftRequest req, ExtrashiftResponse resp, boolean shiftAssigned) {
        return ExtrashiftManagerApprovalDetailDto.builder()
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
