package com.easy1auth.useraccess;

import com.easy1auth.foundation.error.ErrorCode;

/**
 * 用户访问控制（useraccess）模块的错误码常量。
 *
 * <p>集中定义 pool_user 访问控制相关的领域错误码（15xxx 段）：
 * 角色、权限及其层级关系、用户-角色分配等场景的错误描述。</p>
 */
public interface ErrorCodeConstants {
    ErrorCode PERMISSION_CYCLE = new ErrorCode(15000, "权限层级不能形成循环");
    ErrorCode PERMISSION_HAS_CHILDREN = new ErrorCode(15001, "权限下还有子权限，不能删除");
    ErrorCode PERMISSION_INVALID = new ErrorCode(15002, "权限字段无效");
    ErrorCode PERMISSION_PARENT_SELF = new ErrorCode(15003, "不能将自身设为父权限");
    ErrorCode ROLE_CYCLE = new ErrorCode(15004, "角色层级不能形成循环");
    ErrorCode ROLE_HAS_CHILDREN = new ErrorCode(15005, "角色下还有子角色，不能删除");
    ErrorCode ROLE_HAS_USERS = new ErrorCode(15006, "角色下还有用户，不能删除");
    ErrorCode ROLE_INVALID = new ErrorCode(15007, "角色字段或数据范围无效");
    ErrorCode ROLE_PARENT_SELF = new ErrorCode(15008, "不能将自身设为父角色");
    ErrorCode SYSTEM_ROLE_IMMUTABLE_DELETE = new ErrorCode(15009, "内置角色不能删除");
    ErrorCode SYSTEM_ROLE_IMMUTABLE_UPDATE = new ErrorCode(15010, "内置角色不能修改");
    ErrorCode SYSTEM_ROLE_RESERVED = new ErrorCode(15011, "内置角色只能由系统初始化");
    ErrorCode USER_ACCESS_NOT_FOUND = new ErrorCode(15012, "访问控制项不存在");
    ErrorCode USER_ACCESS_USER_NOT_FOUND = new ErrorCode(15013, "用户不存在");
    ErrorCode USER_PERMISSION_NOT_FOUND = new ErrorCode(15014, "权限不存在");
    ErrorCode USER_ROLE_NOT_FOUND = new ErrorCode(15015, "角色不存在");
}
