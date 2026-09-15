package com.easy1auth.system.constant;

import com.easy1auth.framework.common.error.DomainException;
import com.easy1auth.framework.common.error.ErrorCode;

/**
 * 系统管理域错误码常量（12000-13017）。
 *
 * <p>定义管理后台账号/凭证/会话/注册码等业务场景的错误码，
 * 供领域服务抛出 {@link DomainException} 使用。</p>
 */
public interface ErrorCodeConstants {
    ErrorCode ADMINISTRATOR_ACCOUNT_NOT_ACTIVE = new ErrorCode(12000, "管理员账号不存在或未启用");
    ErrorCode ADMINISTRATOR_ACCOUNT_REQUIRED = new ErrorCode(12001, "管理员账号不能为空");
    ErrorCode ADMIN_DISABLED = new ErrorCode(12002, "账号已被禁用");
    ErrorCode ADMIN_EXISTS_EMAIL = new ErrorCode(12003, "该邮箱已被注册");
    ErrorCode ADMIN_EXISTS_USERNAME_OR_EMAIL = new ErrorCode(12004, "用户名或邮箱已被注册");
    ErrorCode ADMIN_SESSION_INVALID = new ErrorCode(12005, "管理员会话已失效");
    ErrorCode CURRENT_PASSWORD_INVALID = new ErrorCode(12006, "当前密码错误");
    ErrorCode EMAIL_INVALID = new ErrorCode(12007, "邮箱格式不正确");
    ErrorCode EMAIL_UNCHANGED = new ErrorCode(12008, "新邮箱不能与当前邮箱相同");
    ErrorCode INVALID_CREDENTIALS = new ErrorCode(12009, "用户名或密码错误");
    ErrorCode INVALID_REFRESH_TOKEN = new ErrorCode(12010, "刷新令牌无效或已失效");
    ErrorCode PASSWORD_WEAK = new ErrorCode(12011, "密码至少8位且必须包含大小写字母和数字");
    ErrorCode PHONE_INVALID = new ErrorCode(12012, "手机号格式不正确");
    ErrorCode SELF_MFA_RESET_DENIED = new ErrorCode(12013, "不能重置自己的 MFA");
    ErrorCode SELF_PASSWORD_RESET_DENIED = new ErrorCode(12014, "不能重置自己的密码");
    ErrorCode SELF_STATUS_CHANGE = new ErrorCode(12015, "不能修改自己的账号状态");
    ErrorCode STATUS_INVALID = new ErrorCode(12016, "账号状态无效");
    ErrorCode USERNAME_INVALID = new ErrorCode(12017, "用户名不能为空且不能超过100字符");
    ErrorCode VERIFICATION_CODE_INVALID = new ErrorCode(12018, "验证码无效或已过期");

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
