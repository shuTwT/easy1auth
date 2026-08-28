package com.easy1auth.adminaccess;

import com.easy1auth.foundation.error.ErrorCode;

/**
 * 管理端权限域（admin-access）错误码常量。
 *
 * <p>错误码统一以 13xxx 为前缀，定义该域内管理角色、权限目录、平台授权相关
 * 的领域异常消息。各常量由 {@link ErrorCode} 承载编码与中文提示，供抛出
 * {@link com.easy1auth.foundation.error.DomainException} 时使用。</p>
 */
public interface ErrorCodeConstants {
    ErrorCode ADMINISTRATOR_TRANSFER_REQUIRED = new ErrorCode(13000, "账号仍是租户唯一管理员，请先完成租户管理员转移");
    ErrorCode ADMIN_ACCOUNT_NOT_FOUND = new ErrorCode(13001, "管理员账号不存在");
    ErrorCode ADMIN_MEMBER_NOT_FOUND = new ErrorCode(13002, "管理员不属于当前租户");
    ErrorCode ADMIN_ROLE_NOT_FOUND = new ErrorCode(13003, "管理员角色不存在");
    ErrorCode MANAGEMENT_PERMISSION_INACTIVE = new ErrorCode(13004, "权限已停用");
    ErrorCode MANAGEMENT_PERMISSION_INVALID = new ErrorCode(13005, "包含未知或非管理端权限");
    ErrorCode MANAGEMENT_PERMISSION_METADATA_INVALID = new ErrorCode(13006, "权限目录元数据无效");
    ErrorCode MANAGEMENT_PERMISSION_SCOPE_INVALID = new ErrorCode(13007, "权限不属于当前作用域");
    ErrorCode PERMISSION_DENIED = new ErrorCode(13008, "权限不足");
    ErrorCode PERMISSION_ESCALATION = new ErrorCode(13009, "不能授予自己不具备的权限");
    ErrorCode PLATFORM_ACCESS_DENIED = new ErrorCode(13010, "无权访问平台资源");
    ErrorCode PLATFORM_PERMISSION_SCOPE_INVALID = new ErrorCode(13011, "权限不属于平台作用域");
    ErrorCode ROLE_NAME_INVALID = new ErrorCode(13012, "角色名称无效");
    ErrorCode SYSTEM_ROLE_IMMUTABLE = new ErrorCode(13013, "系统角色不能修改或删除");
    ErrorCode SYSTEM_TENANT_CONTEXT_INVALID = new ErrorCode(13014, "系统租户上下文无效");
    ErrorCode SYSTEM_TENANT_INVALID = new ErrorCode(13015, "系统租户状态无效");
    ErrorCode TENANT_MEMBERSHIP_ROLE_INVALID = new ErrorCode(13016, "成员角色与租户类型不匹配");
    ErrorCode TENANT_PACKAGE_MISSING = new ErrorCode(13017, "普通租户缺少有效套餐");
}
