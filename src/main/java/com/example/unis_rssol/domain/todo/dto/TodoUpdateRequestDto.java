package com.example.unis_rssol.domain.todo.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TodoUpdateRequestDto {

    @Size(max = 500, message = "내용은 500자 이내로 입력해주세요.")
    private String content;

    private Boolean completed;
}

