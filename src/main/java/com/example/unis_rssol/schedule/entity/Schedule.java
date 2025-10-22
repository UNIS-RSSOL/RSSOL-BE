package com.example.unis_rssol.schedule.entity;
//이건 구조체로사용 DB에 저장되지 않아욤 ^~!^ 그냥 Candidate로 옮김
import com.example.unis_rssol.schedule.DayOfWeek;
import com.example.unis_rssol.schedule.workavailability.WorkAvailability;
import com.example.unis_rssol.store.entity.Store;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "schedule")
public class Schedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL)
    private List<WorkShift> workShifts = new ArrayList<>();
}
