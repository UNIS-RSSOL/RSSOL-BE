package com.rssolplan.api.domain.schedule.extrashift.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExtrashiftRespondDto {
    private String action; // accept 또는 reject
}
