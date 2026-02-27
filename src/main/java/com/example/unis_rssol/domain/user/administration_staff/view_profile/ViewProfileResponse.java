package com.example.unis_rssol.domain.user.administration_staff.view_profile;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class ViewProfileResponse {

    private String username;
    private String profileImageUrl;
    private String status;       // HIRED, ON_LEAVE, RESIGNED
    private String position;     // OWNER, STAFF
    private String storeName;
    private String bankName;
    private String accountNumber;
    private String email;
    private LocalDate hireDate;
    private long daysWorked;
}
