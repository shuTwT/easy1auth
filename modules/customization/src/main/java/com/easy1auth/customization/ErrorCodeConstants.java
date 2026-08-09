package com.easy1auth.customization;

import com.easy1auth.foundation.error.ErrorCode;

public interface ErrorCodeConstants {
    ErrorCode ASSET_URL_INVALID = new ErrorCode(20000, "认证页面资源必须使用 HTTPS URL");
    ErrorCode LEGAL_DOCUMENT_CONTENT_UNSAFE = new ErrorCode(20001, "法律文档包含不安全脚本");
    ErrorCode COLOR_INVALID = new ErrorCode(20002, "颜色必须为六位十六进制值");
    ErrorCode DOMAIN_INVALID = new ErrorCode(20003, "域名格式无效");
    ErrorCode DOMAIN_METHOD_INVALID = new ErrorCode(20004, "域名验证方式无效");
    ErrorCode DOMAIN_NOT_FOUND = new ErrorCode(20005, "域名不存在");
    ErrorCode DOMAIN_VERIFICATION_NOT_AVAILABLE = new ErrorCode(20006, "阶段 6 不提供域名所有权验证或证书托管");
    ErrorCode LOGIN_METHOD_INVALID = new ErrorCode(20007, "至少保留一种登录方式");
    ErrorCode MESSAGE_TEMPLATE_INVALID = new ErrorCode(20008, "消息模板参数无效");
    ErrorCode MESSAGE_TEMPLATE_NOT_FOUND = new ErrorCode(20009, "消息模板不存在");
    ErrorCode STYLE_TEXT_INVALID = new ErrorCode(20010, "登录样式文本无效");
    ErrorCode TEMPLATE_VARIABLE_UNKNOWN = new ErrorCode(20011, "模板包含未声明变量");
    ErrorCode STYLE_CONFIG_INVALID = new ErrorCode(20012, "登录样式配置无效");
}
