package com.cuet_transport_backend.dto;

public record ApiResponse<T>(boolean success, String message, T data, String error) {

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    public static <T> ApiResponse<T> fail(String message, String error) {
        return new ApiResponse<>(false, message, null, error);
    }
}
