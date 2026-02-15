package com.example.unis_rssol.domain.payroll.laborlaw;

import lombok.Getter;

import java.time.LocalDate;

@Getter
public class MinimumWageUpdateRequest {
    private int amount;
    private LocalDate effectiveFrom;
}