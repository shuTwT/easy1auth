package com.easy1auth.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 管理端 JWT 配置（前缀 {@code easy1auth.admin-jwt}）。
 *
 * <p>集中管理 admin_user 访问令牌与刷新令牌的签发信息：issuer/audience 用于校验令牌
 * 归属，secret 用于 HS256 签名，Ttl 用于控制令牌有效期。</p>
 *
 * @param issuer     令牌签发方（issuer），通常是管理端域名的 HTTPS 源
 * @param audience   令牌目标受众（audience），校验令牌是否供管理端使用
 * @param secret     签名密钥，至少 32 字节 UTF-8，用于 HMAC-SHA256 签名
 * @param accessTtl  访问令牌有效期
 * @param refreshTtl 刷新令牌有效期
 */
@ConfigurationProperties("easy1auth.admin-jwt")
public record AdminJwtProperties(String issuer, String audience, String secret, Duration accessTtl,
                                 Duration refreshTtl) {
}
