package com.example.unis_rssol.schedule.extrashift.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExtrashiftCreateDto {
    private Long shiftId;     // 사장 UI에서 선택한 기존 work_shift의 id - 요청하고자 하는 일정 시간대
    private int headcount;    // 필요한 인력 요청 인원 수
    private String note;      // 메모(선택적으로 ㅇㅇ 혹시 필요할까봐 넣어둠)
}
