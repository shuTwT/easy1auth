package com.easy1auth.foundation.error;

/** Error codes shared by infrastructure-level request and server failures. */
public interface ErrorCodeConstants {
    ErrorCode ACCESS_DENIED = new ErrorCode(10000, "没有权限访问");
    ErrorCode AUTHENTICATION_REQUIRED = new ErrorCode(10001, "登录状态无效或已过期");
    ErrorCode DATA_INTEGRITY_CONFLICT = new ErrorCode(10002, "数据已存在或违反关联约束");
    ErrorCode INTERNAL_SERVER_ERROR = new ErrorCode(10003, "服务器内部错误");
    ErrorCode REQUEST_FORMAT_INVALID = new ErrorCode(10004, "请求参数格式错误");
    ErrorCode REQUEST_METHOD_NOT_SUPPORTED = new ErrorCode(10005, "请求方法不支持");
    ErrorCode REQUEST_RESOURCE_NOT_FOUND = new ErrorCode(10006, "请求资源不存在");
    ErrorCode REQUEST_VALIDATION_FAILED = new ErrorCode(10007, "请求参数校验失败");
}
