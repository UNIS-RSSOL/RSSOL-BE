package com.example.unis_rssol.schedule.workavailability;
import com.example.unis_rssol.store.entity.UserStore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;


import java.sql.Timestamp;
import java.time.LocalTime;
@Setter
@Getter
@Builder
@Entity
@Table(name = "work_availability", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_store_id", "day_of_week", "start_time", "end_time"})
})
@NoArgsConstructor  // 기본 생성자
@AllArgsConstructor // 전체 필드 생성자
public class WorkAvailability {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_store_id", nullable = false)
    private UserStore userStore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @CreationTimestamp
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

    public enum DayOfWeek {
        MON, TUE, WED, THU, FRI, SAT, SUN
    }
}