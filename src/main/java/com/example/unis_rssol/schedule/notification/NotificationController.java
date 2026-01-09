package com.example.unis_rssol.schedule.notification;

import com.example.unis_rssol.schedule.notification.dto.NotificationResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class NotificationController {
    private final NotificationService service;


    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationResponseDto>> getNotifications(@AuthenticationPrincipal Long userId) {
        List<NotificationResponseDto> dtos = service.getNotifications(userId);

        return ResponseEntity.ok(dtos);
    }
}
