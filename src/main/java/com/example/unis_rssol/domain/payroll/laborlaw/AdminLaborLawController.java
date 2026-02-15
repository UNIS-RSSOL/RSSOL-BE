package com.example.unis_rssol.domain.payroll.laborlaw;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/labor-law")
@RequiredArgsConstructor
//@AdminOnly   // (시큐리티 애노테이션)
public class AdminLaborLawController {

    private final LaborLawConfigService service;

    /* 최저시급 갱신용

     */
    @PostMapping("/minimum-wage")
    public void updateMinimumWage(@RequestBody MinimumWageUpdateRequest request) {
        service.updateMinimumWage(
                request.getAmount(),
                request.getEffectiveFrom()
        );
    }
}
