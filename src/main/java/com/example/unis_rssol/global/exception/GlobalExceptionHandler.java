package com.example.unis_rssol.global.exception;

import com.example.unis_rssol.global.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 400 Bad Request - 잘못된 시간 범위 등
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Object> handleBadRequest(IllegalArgumentException ex) {
        return ApiResponse.error("INVALID_TIME_RANGE", ex.getMessage());
    }

    // 401 Unauthorized - JWT 토큰 문제
    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ApiResponse<Object> handleUnauthorized(UnauthorizedException ex) {
        return ApiResponse.error("UNAUTHORIZED", ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ApiResponse<Object> handleForbidden(ForbiddenException ex) {
        return ApiResponse.error("FORBIDDEN", ex.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Object> handleNotFound(NotFoundException ex) {
        return ApiResponse.error("NOT_FOUND", ex.getMessage());
    }
}
