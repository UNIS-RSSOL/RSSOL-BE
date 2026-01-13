package com.example.unis_rssol.schedule.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
        select n
        from Notification n
        left join fetch n.store
        where n.userId = :userId
        order by n.createdAt desc
    """)
    List<Notification> findByUserIdWithStore(Long userId);

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
}
