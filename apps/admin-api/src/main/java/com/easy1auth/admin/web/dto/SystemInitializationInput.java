package com.easy1auth.admin.web.dto;

/**
 * 系统初始化输入。
 *
 * @param account  系统管理员登录账号，使用邮箱地址
 * @param password 系统管理员初始密码
 */
public record SystemInitializationInput(String account, String password) {
}
