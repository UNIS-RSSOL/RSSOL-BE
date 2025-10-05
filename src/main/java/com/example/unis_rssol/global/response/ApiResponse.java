package com.example.unis_rssol.global.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String error;
    private String message;
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
