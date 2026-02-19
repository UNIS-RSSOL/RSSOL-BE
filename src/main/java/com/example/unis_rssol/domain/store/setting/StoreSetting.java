package com.example.unis_rssol.domain.store.setting;

import com.example.unis_rssol.domain.store.Store;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "store_setting")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StoreSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false, unique = true)
    private Store store;

    @Column(nullable = false)
    private LocalTime openTime;

    @Column(nullable = false)
    private LocalTime closeTime;

    // 세그먼트 사용 여부 (true: 파트타임 구간 나눔, false: 30분 단위 N명 배정)
    @Column(nullable = false)
    private boolean useSegments;


    // 브레이크타임
    @Column(nullable = false)
    private boolean hasBreakTime;

    private LocalTime breakStartTime;
    private LocalTime breakEndTime;

    // 세그먼트 목록
    @OneToMany(mappedBy = "storeSetting", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<StoreSettingSegment> segments = new ArrayList<>();

    @CreationTimestamp
    private Timestamp createdAt;

    @UpdateTimestamp
    private Timestamp updatedAt;

    // 편의 메서드
    public void addSegment(StoreSettingSegment segment) {
        segments.add(segment);
        segment.setStoreSetting(this);
    }

    public void clearSegments() {
        segments.clear();
    }
}
