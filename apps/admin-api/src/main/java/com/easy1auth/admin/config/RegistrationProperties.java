package com.easy1auth.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 自助注册配置（前缀 {@code easy1auth.registration}）。
 *
 * <p>控制管理端开放注册时的行为：是否在注册流程中暴露（返回）验证码，以及注册/验证码
 * 邮件的发件人地址。</p>
 *
 * @param exposeCode  是否把验证码直接返回给前端（开发调试用；生产环境应关闭）
 * @param fromAddress 注册与验证码邮件的发件人地址
 */
@ConfigurationProperties("easy1auth.registration")
public record RegistrationProperties(boolean exposeCode, String fromAddress) {
}
