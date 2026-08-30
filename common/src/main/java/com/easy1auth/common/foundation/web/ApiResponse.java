package com.easy1auth.common.foundation.web;

import com.easy1auth.common.foundation.error.ErrorCode;

/**
 * 统一 Web 响应包装（面向接口层的只读 DTO）。
 *
 * <p>code 为业务状态码（0 表示成功，非 0 为业务错误码），data 为业务数据，
 * msg 为提示信息。所有管理端接口的响应统一使用该结构。</p>
 *
 * @param <T>  业务数据类型
 * @param code 业务状态码：0 表示成功，非 0 对应 {@link ErrorCode} 错误码
 * @param data 业务数据（失败时为 null）
 * @param msg  提示信息（成功默认为"成功"，失败为错误信息）
 */
public record ApiResponse<T>(int code, T data, String msg) {
    /** 成功状态码 */
    public static final int SUCCESS_CODE = 0;

    /** 构建成功响应（默认提示"成功"）。 */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(SUCCESS_CODE, data, "成功");
    }

    /** 构建成功响应（自定义提示信息）。 */
    public static <T> ApiResponse<T> ok(T data, String msg) {
        return new ApiResponse<>(SUCCESS_CODE, data, msg);
    }

    /** 构建失败响应：使用错误码的编号与默认提示信息。 */
    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode.code(), null, errorCode.message());
    }
}
