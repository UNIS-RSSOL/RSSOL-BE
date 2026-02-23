package com.example.unis_rssol.domain.todo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    // 매장 전체 할일 조회 (STORE 타입)
    List<Todo> findByStore_IdAndDateAndTodoType(Long storeId, LocalDate date, Todo.TodoType todoType);

    // 인수인계 조회 (HANDOVER 타입, 매장 기준)
    List<Todo> findByStore_IdAndDateAndTodoTypeOrderByCreatedAtDesc(Long storeId, LocalDate date, Todo.TodoType todoType);

    // 내 할일 조회 (PERSONAL 타입, 사용자 기준)
    List<Todo> findByUser_IdAndStore_IdAndDateAndTodoType(Long userId, Long storeId, LocalDate date, Todo.TodoType todoType);

    // 특정 날짜의 모든 할일 조회 (매장 기준, 타입별로 분류할 수 있도록)
    @Query("SELECT t FROM Todo t WHERE t.store.id = :storeId AND t.date = :date " +
            "AND (t.todoType IN ('STORE', 'HANDOVER') OR (t.todoType = 'PERSONAL' AND t.user.id = :userId)) " +
            "ORDER BY t.todoType, t.createdAt DESC")
    List<Todo> findAllTodosForDate(@Param("storeId") Long storeId,
                                   @Param("userId") Long userId,
                                   @Param("date") LocalDate date);
}

