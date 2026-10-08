package com.easy1auth.tenant.constant;

import com.easy1auth.framework.common.error.ErrorCode;

/**
 * 用户池模块错误码常量集合（错误码区间 11000-11031）。
 *
 * <p>集中定义用户池域内各业务校验失败对应的错误码与中文提示，
 * 供领域服务抛出 DomainException 时使用，由统一异常处理转换为响应。</p>
 */
public interface ErrorCodeConstants {
    ErrorCode ADMINISTRATOR_ACCOUNT_REQUIRED = new ErrorCode(11000, "管理员账号不能为空");
    ErrorCode USER_POOL_ACCESS_DENIED = new ErrorCode(11001, "无权访问所选用户池");
    ErrorCode USER_POOL_ADMINISTRATOR_MISSING = new ErrorCode(11002, "用户池缺少有效管理员");
    ErrorCode USER_POOL_ADMINISTRATOR_TARGET_CURRENT = new ErrorCode(11003, "目标账号已是当前用户池管理员");
    ErrorCode USER_POOL_CONTEXT_MISMATCH = new ErrorCode(11004, "数据用户池与当前用户池不一致");
    ErrorCode USER_POOL_CONTEXT_REQUIRED = new ErrorCode(11005, "用户池上下文不可用");
    ErrorCode USER_POOL_ID_REQUIRED = new ErrorCode(11006, "用户池不能为空");
    ErrorCode USER_POOL_INVALID = new ErrorCode(11007, "用户池标识无效");
    ErrorCode USER_POOL_MEMBERSHIP_ROLE_INVALID = new ErrorCode(11008, "成员角色与用户池类型不匹配");
    ErrorCode USER_POOL_NAME_INVALID = new ErrorCode(11009, "用户池名称不能为空且不能超过200个字符");
    ErrorCode USER_POOL_NOT_ACTIVE = new ErrorCode(11010, "停用的用户池不能转移管理员");
    ErrorCode USER_POOL_NOT_FOUND_ACTIVE = new ErrorCode(11011, "用户池不存在或未启用");
    ErrorCode USER_POOL_NOT_FOUND_ORDINARY = new ErrorCode(11012, "普通用户池不存在或已删除");
    ErrorCode USER_POOL_PACKAGE_ASSIGNED_DELETE = new ErrorCode(11013, "已分配给普通用户池的套餐不能删除");
    ErrorCode USER_POOL_PACKAGE_ASSIGNED_SUSPEND = new ErrorCode(11014, "已分配给普通用户池的套餐不能停用");
    ErrorCode USER_POOL_PACKAGE_CODE_EXISTS = new ErrorCode(11015, "用户池套餐编码已存在");
    ErrorCode USER_POOL_PACKAGE_CODE_INVALID = new ErrorCode(11016, "用户池套餐编码无效");
    ErrorCode USER_POOL_PACKAGE_DEFAULT_DELETE_FORBIDDEN = new ErrorCode(11017, "默认用户池套餐不能删除");
    ErrorCode USER_POOL_PACKAGE_DEFAULT_MUST_BE_ACTIVE = new ErrorCode(11018, "默认用户池套餐必须启用");
    ErrorCode USER_POOL_PACKAGE_DEFAULT_NOT_ACTIVE = new ErrorCode(11019, "默认用户池套餐不存在或未启用");
    ErrorCode USER_POOL_PACKAGE_ID_INVALID = new ErrorCode(11020, "用户池套餐 ID 必须为正数");
    ErrorCode USER_POOL_PACKAGE_NAME_EXISTS = new ErrorCode(11021, "用户池套餐名称已存在");
    ErrorCode USER_POOL_PACKAGE_NAME_INVALID = new ErrorCode(11022, "用户池套餐名称无效");
    ErrorCode USER_POOL_PACKAGE_NOT_ACTIVE = new ErrorCode(11023, "用户池套餐不存在或未启用");
    ErrorCode USER_POOL_PACKAGE_NOT_FOUND = new ErrorCode(11024, "用户池套餐不存在");
    ErrorCode USER_POOL_PACKAGE_PERMISSION_CODES_INVALID = new ErrorCode(11025, "用户池套餐权限编码无效");
    ErrorCode USER_POOL_PACKAGE_QUOTA_INVALID = new ErrorCode(11026, "用户池套餐配额必须为正数");
    ErrorCode USER_POOL_PACKAGE_REQUIRED = new ErrorCode(11027, "用户池套餐不能为空");
    ErrorCode USER_POOL_PACKAGE_STATUS_INVALID = new ErrorCode(11028, "用户池套餐状态无效");
    ErrorCode USER_POOL_QUOTA_APPLICATIONS_UNAVAILABLE = new ErrorCode(11029, "当前用户池没有可用的应用额度套餐，请切换到普通用户池或先为用户池配置启用中的套餐");
    ErrorCode USER_POOL_QUOTA_USERS_UNAVAILABLE = new ErrorCode(11030, "当前用户池没有可用的用户额度套餐，请切换到普通用户池或先为用户池配置启用中的套餐");
    ErrorCode USER_POOL_STATUS_INVALID = new ErrorCode(11031, "用户池状态只能是 active 或 suspended");
}
