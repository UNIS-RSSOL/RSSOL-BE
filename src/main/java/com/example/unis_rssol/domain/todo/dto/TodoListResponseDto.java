package com.example.unis_rssol.domain.todo.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TodoListResponseDto {
    private LocalDate date;
    private List<TodoResponseDto> storeTodos;      // 매장 전체 할일
    private List<TodoResponseDto> handoverTodos;   // 인수인계
    private List<TodoResponseDto> personalTodos;   // 내 할일
}

