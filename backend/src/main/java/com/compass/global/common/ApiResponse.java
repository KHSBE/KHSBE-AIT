package com.compass.global.common;

// 공통 API 응답 규격
public record ApiResponse<T>(
        boolean success,
        String message,
        T data
) {
    // 성공 응답
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "요청에 성공했습니다.", data);
    }

    // 실패 응답
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
}