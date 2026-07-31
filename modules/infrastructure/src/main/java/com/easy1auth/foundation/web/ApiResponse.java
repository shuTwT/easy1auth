package com.easy1auth.foundation.web;

public record ApiResponse<T>(int code, T data, String msg) {
    public static final int SUCCESS_CODE = 0;
    public static final int ERROR_CODE = 10000;

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(SUCCESS_CODE, data, "成功");
    }

    public static <T> ApiResponse<T> ok(T data, String msg) {
        return new ApiResponse<>(SUCCESS_CODE, data, msg);
    }

    public static ApiResponse<Void> error(String msg) {
        return new ApiResponse<>(ERROR_CODE, null, msg);
    }
}
