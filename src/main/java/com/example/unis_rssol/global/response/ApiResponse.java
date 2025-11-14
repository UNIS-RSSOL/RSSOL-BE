package com.example.unis_rssol.global.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "API 공통 응답 구조")
public class ApiResponse<T> {
    @Schema(description = "성공 여부", example = "false")
    private boolean success;

    @Schema(description = "에러 코드", example = "BAD_REQUEST")
    private String error;
    @Schema(description = "에러/메시지 내용", example = "잘못된 요청입니다.")
    private String message;
    @Schema(description = "응답 데이터")
    private T data;

    // 성공용
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, null, null, data);
    }

    // 에러용
    public static <T> ApiResponse<T> error(String error, String message) {
        return new ApiResponse<>(false, error, message, null);
    }
}
