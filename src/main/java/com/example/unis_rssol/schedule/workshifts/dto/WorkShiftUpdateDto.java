package com.example.unis_rssol.schedule.workshifts.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WorkShiftUpdateDto {
    private LocalDateTime startDatetime;
    private LocalDateTime endDatetime;
}
