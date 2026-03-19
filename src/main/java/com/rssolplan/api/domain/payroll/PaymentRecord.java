package com.rssolplan.api.domain.payroll;

import com.rssolplan.api.domain.store.UserStore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 급여 지급 기록
 * - Owner가 직원에게 급여를 지급했는지 여부를 월별로 관리
 * - 체크박스로 지급/미지급 상태 토글 가능
 */
@Entity
@Table(name = "payment_record",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_store_id", "year", "month"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_store_id", nullable = false)
    private UserStore userStore;

    @Column(name = "year", nullable = false)
    private int year;

    @Column(name = "month", nullable = false)
    private int month;

    @Column(name = "is_paid", nullable = false)
    @Builder.Default
    private boolean isPaid = false;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "paid_by_user_id")
    private Long paidByUserId;  // 지급을 처리한 Owner의 ID

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 급여 지급 상태 업데이트
     *
     * @param isPaid      지급 여부
     * @param ownerUserId 지급 처리한 Owner의 ID
     */
    public void updatePaymentStatus(boolean isPaid, Long ownerUserId) {
        this.isPaid = isPaid;
        this.paidByUserId = ownerUserId;
        if (isPaid) {
            this.paidAt = LocalDateTime.now();
        } else {
            this.paidAt = null;
        }
        this.updatedAt = LocalDateTime.now();
    }
}

