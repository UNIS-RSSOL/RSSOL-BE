package com.example.unis_rssol.domain.todo.dto;

import com.example.unis_rssol.domain.todo.Todo;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TodoResponseDto {
    private Long id;
    private LocalDate date;
    private Todo.TodoType todoType;
    private String content;
    private Boolean completed;
    private Long authorId;
    private String authorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TodoResponseDto from(Todo todo) {
        return TodoResponseDto.builder()
                .id(todo.getId())
                .date(todo.getDate())
                .todoType(todo.getTodoType())
                .content(todo.getContent())
                .completed(todo.getCompleted())
                .authorId(todo.getUser().getId())
                .authorName(todo.getUser().getUsername())
                .createdAt(todo.getCreatedAt())
                .updatedAt(todo.getUpdatedAt())
                .build();
    }
}

