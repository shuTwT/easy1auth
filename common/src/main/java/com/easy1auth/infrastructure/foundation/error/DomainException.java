package com.easy1auth.infrastructure.foundation.error;

/**
 * 领域异常：业务规则校验失败的统一异常类型。
 *
 * <p>携带一个稳定的 {@link ErrorCode}，异常消息直接使用其面向用户的提示信息，
 * 便于接口层直接转换为统一响应结构。</p>
 */
public final class DomainException extends RuntimeException {
    /** 关联的业务错误码 */
    private final ErrorCode errorCode;

    /**
     * 以指定错误码构造领域异常。
     *
     * @param errorCode 业务错误码（不能为 null）
     */
    public DomainException(ErrorCode errorCode) {
        super(java.util.Objects.requireNonNull(errorCode, "errorCode").message());
        this.errorCode = errorCode;
    }

    /** 返回关联的业务错误码。 */
    public ErrorCode errorCode() {
        return errorCode;
    }

    /** 返回业务错误码的编号。 */
    public int code() {
        return errorCode.code();
    }
}
