package com.example.unis_rssol.schedule.extrashift.dto;

import com.example.unis_rssol.schedule.extrashift.entity.ExtrashiftRequest;
import com.example.unis_rssol.schedule.extrashift.entity.ExtrashiftResponse;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ExtrashiftResponseDetailDto {

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
    private String workerAction;
    private String managerApproval;

    private String createdAt;

    public static ExtrashiftResponseDetailDto of(ExtrashiftRequest req, ExtrashiftResponse resp) {
        return ExtrashiftResponseDetailDto.builder()
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
