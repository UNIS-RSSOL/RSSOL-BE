package com.example.unis_rssol.domain.todo;

import com.example.unis_rssol.domain.todo.dto.TodoCreateRequestDto;
import com.example.unis_rssol.domain.todo.dto.TodoListResponseDto;
import com.example.unis_rssol.domain.todo.dto.TodoResponseDto;
import com.example.unis_rssol.domain.todo.dto.TodoUpdateRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/todos")
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;

    /**
     * 특정 날짜의 모든 할일 조회 (매장 전체, 인수인계, 내 할일)
     * GET /api/todos?date=2024-01-15
     */
    @GetMapping
    public ResponseEntity<TodoListResponseDto> getTodosByDate(
            @AuthenticationPrincipal Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        TodoListResponseDto response = todoService.getTodosByDate(userId, date);
        return ResponseEntity.ok(response);
    }

    /**
     * 할일 생성
     * POST /api/todos
     */
    @PostMapping
    public ResponseEntity<TodoResponseDto> createTodo(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody TodoCreateRequestDto request) {
        TodoResponseDto response = todoService.createTodo(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 할일 수정
     * PUT /api/todos/{todoId}
     */
    @PutMapping("/{todoId}")
    public ResponseEntity<TodoResponseDto> updateTodo(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long todoId,
            @Valid @RequestBody TodoUpdateRequestDto request) {
        TodoResponseDto response = todoService.updateTodo(userId, todoId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 할일 삭제
     * DELETE /api/todos/{todoId}
     */
    @DeleteMapping("/{todoId}")
    public ResponseEntity<Void> deleteTodo(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long todoId) {
        todoService.deleteTodo(userId, todoId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 할일 완료 토글
     * PATCH /api/todos/{todoId}/toggle
     */
    @PatchMapping("/{todoId}/toggle")
    public ResponseEntity<TodoResponseDto> toggleTodoCompleted(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long todoId) {
        TodoResponseDto response = todoService.toggleTodoCompleted(userId, todoId);
        return ResponseEntity.ok(response);
    }
}

