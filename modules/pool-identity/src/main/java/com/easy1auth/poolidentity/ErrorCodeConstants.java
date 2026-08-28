package com.easy1auth.poolidentity;

import com.easy1auth.infrastructure.foundation.error.ErrorCode;

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
    ErrorCode CURRENT_PASSWORD_INVALID_POOL_USER = new ErrorCode(14000, "原密码错误");
    ErrorCode DIRECTORY_GROUP_NOT_FOUND = new ErrorCode(14001, "用户组不存在");
    ErrorCode DIRECTORY_ITEM_NOT_FOUND = new ErrorCode(14002, "目录项不存在");
    ErrorCode DIRECTORY_POSITION_NOT_FOUND = new ErrorCode(14003, "岗位不存在");
    ErrorCode DIRECTORY_USER_NOT_FOUND = new ErrorCode(14004, "用户不存在");
    ErrorCode ENTERPRISE_IDENTITY_MANAGED_DEPARTMENT = new ErrorCode(14005, "该部门由企业身份源管理，请在身份源中修改");
    ErrorCode ENTERPRISE_IDENTITY_MANAGED_USER = new ErrorCode(14006, "该用户由企业身份源管理，请在身份源中修改");
    ErrorCode GROUP_CYCLE = new ErrorCode(14007, "用户组层级不能形成循环");
    ErrorCode GROUP_HAS_CHILDREN = new ErrorCode(14008, "用户组下仍有子组，不能删除");
    ErrorCode GROUP_INVALID = new ErrorCode(14009, "用户组名称或类型无效");
    ErrorCode GROUP_PARENT_SELF = new ErrorCode(14010, "不能将自身设为父组");
    ErrorCode PASSWORD_WEAK = new ErrorCode(14011, "密码至少8位");
    ErrorCode POOL_USER_INVALID = new ErrorCode(14012, "用户名、姓名以及邮箱或手机号为必填项");
    ErrorCode POOL_USER_NOT_FOUND = new ErrorCode(14013, "用户不存在");
    ErrorCode POSITION_INVALID = new ErrorCode(14014, "岗位名称和编码不能为空");
    ErrorCode TENANT_USER_LIMIT = new ErrorCode(14015, "已达到用户数量上限");
    ErrorCode USER_STATUS_INVALID = new ErrorCode(14016, "用户状态无效");
}
