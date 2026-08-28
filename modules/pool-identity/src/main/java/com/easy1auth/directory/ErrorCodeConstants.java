package com.easy1auth.directory;

import com.easy1auth.foundation.error.ErrorCode;

/**
 * 目录（directory）模块的错误码常量。
 *
 * <p>集中定义 pool_user 目录相关的领域错误码（14xxx 段）：
 * 用户、用户组、岗位以及企业身份源托管、密码与配额等场景的错误描述。</p>
 */
public interface ErrorCodeConstants {
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
