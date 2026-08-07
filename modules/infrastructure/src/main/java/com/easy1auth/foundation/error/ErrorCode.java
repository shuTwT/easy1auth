package com.easy1auth.foundation.error;

import java.util.Objects;

/**
 * Stable business error code and its default user-facing message.
 *
 * <p>HTTP status is deliberately not part of this type. Business errors are
 * transported independently from the protocol status used by the caller.</p>
 */
public final class ErrorCode {
    private final int code;
    private final String message;

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

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ErrorCode that)) return false;
        return code == that.code && message.equals(that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, message);
    }

    @Override
    public String toString() {
        return "ErrorCode[code=" + code + ", message=" + message + "]";
    }
}
