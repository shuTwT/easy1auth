package com.easy1auth.framework.tenant.error;

import com.easy1auth.framework.common.error.ErrorCode;

/**
 * 用户池模块错误码常量集合（错误码区间 11000-11031）。
 *
 * <p>集中定义用户池域内各业务校验失败对应的错误码与中文提示，
 * 供领域服务抛出 DomainException 时使用，由统一异常处理转换为响应。</p>
 */
public interface ErrorCodeConstants {
    ErrorCode USER_POOL_CONTEXT_MISMATCH = new ErrorCode(11004, "数据用户池与当前用户池不一致");
}
