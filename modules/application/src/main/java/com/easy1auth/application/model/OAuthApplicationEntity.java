package com.easy1auth.application.model;

import com.easy1auth.common.persistence.model.BaseEntity;
import com.easy1auth.common.persistence.model.BaseTenantEntity;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.List;

/**
 * OAuth2 应用实体（对应 oauth_application 表）。
 *
 * <p>表示租户下注册的一个 OAuth2 / OIDC 客户端应用，配置其类型、回调地址、
 * 授权类型、作用域与令牌有效期等。继承 {@link BaseEntity} 与
 * {@link BaseTenantEntity}，具备通用字段与租户隔离能力。</p>
 */
@Entity
@Table(name = "oauth_application")
public interface OAuthApplicationEntity extends BaseEntity, BaseTenantEntity {

    /** 应用名称（同一租户内唯一） */
    String name();

    /** 应用 Logo（可为空） */
    @Nullable String logo();

    /** 应用描述（可为空） */
    @Nullable String description();

    /** 应用类型：web（Web）/ native（原生）/ spa（单页应用）/ machine（机器） */
    String type();

    /** 客户端 ID（唯一标识，形如 app_xxxxxxxx） */
    @Column(name = "client_id")
    String clientId();

    /** 客户端密钥的哈希（公共客户端不保存密钥，可为空） */
    @Column(name = "client_secret_hash")
    @Nullable String clientSecretHash();

    /** 授权码回调地址列表（序列化存储） */
    @Serialized
    @Column(name = "redirect_uris")
    List<String> redirectUris();

    /** 登出后的跳转地址列表（序列化存储） */
    @Serialized
    @Column(name = "post_logout_redirect_uris")
    List<String> postLogoutRedirectUris();

    /** 允许的授权类型列表（authorization_code / refresh_token / client_credentials） */
    @Serialized
    @Column(name = "allowed_grant_types")
    List<String> allowedGrantTypes();

    /** 允许申请的作用域列表（序列化存储） */
    @Serialized
    List<String> scopes();

    /** 是否强制使用 PKCE（公共客户端必须开启） */
    @Column(name = "require_pkce")
    boolean requirePkce();

    /** 授权前是否要求用户同意（显示授权确认页） */
    @Column(name = "require_consent")
    boolean requireConsent();

    /** 访问令牌有效期（秒，默认 900） */
    @Column(name = "access_token_lifetime")
    int accessTokenLifetime();

    /** 刷新令牌有效期（秒，默认 2592000） */
    @Column(name = "refresh_token_lifetime")
    int refreshTokenLifetime();

    /** 应用状态：active（正常）/ disabled（禁用） */
    String status();

    /** 创建时间 */
    @Column(name = "created_at")
    Instant createdAt();

    /** 最后更新时间 */
    @Column(name = "updated_at")
    Instant updatedAt();
}
