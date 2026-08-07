package com.easy1auth.tenant;

import com.easy1auth.foundation.error.ErrorCode;

public interface ErrorCodeConstants {
    ErrorCode ADMINISTRATOR_ACCOUNT_REQUIRED = new ErrorCode(11000, "管理员账号不能为空");
    ErrorCode TENANT_ACCESS_DENIED = new ErrorCode(11001, "无权访问所选租户");
    ErrorCode TENANT_ADMINISTRATOR_MISSING = new ErrorCode(11002, "租户缺少有效管理员");
    ErrorCode TENANT_ADMINISTRATOR_TARGET_CURRENT = new ErrorCode(11003, "目标账号已是当前租户管理员");
    ErrorCode TENANT_CONTEXT_MISMATCH = new ErrorCode(11004, "数据租户与当前租户不一致");
    ErrorCode TENANT_CONTEXT_REQUIRED = new ErrorCode(11005, "租户上下文不可用");
    ErrorCode TENANT_ID_REQUIRED = new ErrorCode(11006, "租户不能为空");
    ErrorCode TENANT_INVALID = new ErrorCode(11007, "租户标识无效");
    ErrorCode TENANT_MEMBERSHIP_ROLE_INVALID = new ErrorCode(11008, "成员角色与租户类型不匹配");
    ErrorCode TENANT_NAME_INVALID = new ErrorCode(11009, "租户名称不能为空且不能超过200个字符");
    ErrorCode TENANT_NOT_ACTIVE = new ErrorCode(11010, "停用的租户不能转移管理员");
    ErrorCode TENANT_NOT_FOUND_ACTIVE = new ErrorCode(11011, "租户不存在或未启用");
    ErrorCode TENANT_NOT_FOUND_ORDINARY = new ErrorCode(11012, "普通租户不存在或已删除");
    ErrorCode TENANT_PACKAGE_ASSIGNED_DELETE = new ErrorCode(11013, "已分配给普通租户的套餐不能删除");
    ErrorCode TENANT_PACKAGE_ASSIGNED_SUSPEND = new ErrorCode(11014, "已分配给普通租户的套餐不能停用");
    ErrorCode TENANT_PACKAGE_CODE_EXISTS = new ErrorCode(11015, "租户套餐编码已存在");
    ErrorCode TENANT_PACKAGE_CODE_INVALID = new ErrorCode(11016, "租户套餐编码无效");
    ErrorCode TENANT_PACKAGE_DEFAULT_DELETE_FORBIDDEN = new ErrorCode(11017, "默认租户套餐不能删除");
    ErrorCode TENANT_PACKAGE_DEFAULT_MUST_BE_ACTIVE = new ErrorCode(11018, "默认租户套餐必须启用");
    ErrorCode TENANT_PACKAGE_DEFAULT_NOT_ACTIVE = new ErrorCode(11019, "默认租户套餐不存在或未启用");
    ErrorCode TENANT_PACKAGE_ID_INVALID = new ErrorCode(11020, "租户套餐 ID 必须为正数");
    ErrorCode TENANT_PACKAGE_NAME_EXISTS = new ErrorCode(11021, "租户套餐名称已存在");
    ErrorCode TENANT_PACKAGE_NAME_INVALID = new ErrorCode(11022, "租户套餐名称无效");
    ErrorCode TENANT_PACKAGE_NOT_ACTIVE = new ErrorCode(11023, "租户套餐不存在或未启用");
    ErrorCode TENANT_PACKAGE_NOT_FOUND = new ErrorCode(11024, "租户套餐不存在");
    ErrorCode TENANT_PACKAGE_PERMISSION_CODES_INVALID = new ErrorCode(11025, "租户套餐权限编码无效");
    ErrorCode TENANT_PACKAGE_QUOTA_INVALID = new ErrorCode(11026, "租户套餐配额必须为正数");
    ErrorCode TENANT_PACKAGE_REQUIRED = new ErrorCode(11027, "租户套餐不能为空");
    ErrorCode TENANT_PACKAGE_STATUS_INVALID = new ErrorCode(11028, "租户套餐状态无效");
    ErrorCode TENANT_QUOTA_APPLICATIONS_UNAVAILABLE = new ErrorCode(11029, "当前租户没有可用的应用额度套餐，请切换到普通租户或先为租户配置启用中的套餐");
    ErrorCode TENANT_QUOTA_USERS_UNAVAILABLE = new ErrorCode(11030, "当前租户没有可用的用户额度套餐，请切换到普通租户或先为租户配置启用中的套餐");
    ErrorCode TENANT_STATUS_INVALID = new ErrorCode(11031, "租户状态只能是 active 或 suspended");
}
