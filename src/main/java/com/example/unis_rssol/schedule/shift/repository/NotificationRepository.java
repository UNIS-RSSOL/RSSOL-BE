package com.example.unis_rssol.schedule.shift.repository;

import com.example.unis_rssol.schedule.shift.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
}
