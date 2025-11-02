package com.example.unis_rssol.schedule.workavailability;

import com.example.unis_rssol.global.auth.annotation.OwnerOnly;
import com.example.unis_rssol.schedule.workavailability.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@Slf4j
@RequestMapping("/api")
public class WorkAvailabilityController {

    private final WorkAvailabilityService service;

    public WorkAvailabilityController(WorkAvailabilityService service) {
        this.service = service;
    }

    @PostMapping("/me/availabilities")
    public ResponseEntity<WorkAvailabilityCreateResponseDto> createAvailability(
            @AuthenticationPrincipal Long userId,
            @RequestBody WorkAvailabilityRequestDto request) {

        WorkAvailabilityCreateResponseDto response = service.createAvailabilities(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/me/availabilities")
    public ResponseEntity<WorkAvailabilityGetResponseDto> getAvailability(
            @AuthenticationPrincipal Long userId){
        log.debug("Get availability for userId={}", userId);
        WorkAvailabilityGetResponseDto response = service.getAvailability(userId);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @PutMapping("/me/availabilities")
    public ResponseEntity<List<WorkAvailabilityPatchResponseDto>> patchAvailability(
            @AuthenticationPrincipal Long userId,
            @RequestBody WorkAvailabilityRequestDto request) {

        List<WorkAvailabilityPatchResponseDto> response = service.replaceAvailabilities(userId, request);
        return ResponseEntity.ok(response);
    }

    @OwnerOnly
    @GetMapping("/{storeId}/availabilities")
    public ResponseEntity<List<WorkAvailabilityAllResponseDto>> getAllAvailability(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long storeId
    ){
        List<WorkAvailabilityAllResponseDto> response = service.getAllAvailability(userId, storeId);
        return ResponseEntity.ok(response);
    }

}