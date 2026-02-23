package com.example.unis_rssol.domain.todo.dto;

import com.example.unis_rssol.domain.todo.Todo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TodoCreateRequestDto {

    @NotNull(message = "날짜는 필수입니다.")
    private LocalDate date;

    @NotNull(message = "할일 타입은 필수입니다.")
    private Todo.TodoType todoType;

    @NotBlank(message = "내용은 필수입니다.")
    @Size(max = 500, message = "내용은 500자 이내로 입력해주세요.")
    private String content;
}

