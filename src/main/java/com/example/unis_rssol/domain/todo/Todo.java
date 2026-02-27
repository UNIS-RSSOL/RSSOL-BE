package com.example.unis_rssol.domain.todo;

import com.example.unis_rssol.domain.store.Store;
import com.example.unis_rssol.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "todo",
        indexes = {
                @Index(name = "idx_todo_store_date", columnList = "store_id, date"),
                @Index(name = "idx_todo_user_date", columnList = "user_id, date"),
                @Index(name = "idx_todo_type", columnList = "todo_type")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Todo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;  // 작성자

    @Column(nullable = false)
    private LocalDate date;  // 해당 날짜

    @Enumerated(EnumType.STRING)
    @Column(name = "todo_type", nullable = false, length = 20)
    private TodoType todoType;

    public enum TodoType {
        STORE,      // 매장 전체 (OWNER만 추가 가능)
        HANDOVER,   // 인수인계 (OWNER, STAFF 모두 가능)
        PERSONAL    // 내 할일 (본인만 가능)
    }

    @Column(nullable = false, length = 500)
    private String content;

    @Column(nullable = false)
    @Builder.Default
    private Boolean completed = false;

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
}

