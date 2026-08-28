package com.easy1auth.infrastructure.foundation.error;

import java.util.Objects;

/**
 * 稳定的业务错误码及其默认面向用户的提示信息。
 *
 * <p>HTTP 状态码刻意不包含在该类型中：业务错误独立于调用方使用的协议状态传输，
 * 便于跨协议、跨场景复用同一套错误码。</p>
 */
public final class ErrorCode {
    /** 业务错误码（正整数） */
    private final int code;
    /** 面向用户的默认提示信息 */
    private final String message;

    /**
     * 构造错误码：要求错误码为正整数、提示信息非空。
     *
     * @param code    业务错误码（必须为正整数）
     * @param message 默认提示信息（必须非空）
     */
    public ErrorCode(int code, String message) {
        if (code <= 0) {
            throw new IllegalArgumentException("error code must be positive");
        }
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("error message must not be blank");
        }
        this.code = code;
        this.message = message;
    }

    /** 返回业务错误码。 */
    public int code() {
        return code;
    }

    /** 返回面向用户的默认提示信息。 */
    public String message() {
        return message;
    }

    /** 判断两个错误码是否相等（编号与提示信息均相同）。 */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ErrorCode that)) {
            return false;
        }
        return code == that.code && message.equals(that.message);
    }

    /** 基于编号与提示信息计算哈希值。 */
    @Override
    public int hashCode() {
        return Objects.hash(code, message);
    }

    /** 返回形如 "ErrorCode[code=..., message=...]" 的字符串表示。 */
    @Override
    public String toString() {
        return "ErrorCode[code=" + code + ", message=" + message + "]";
    }
}
