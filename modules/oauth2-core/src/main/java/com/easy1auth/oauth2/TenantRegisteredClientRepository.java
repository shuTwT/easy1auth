package com.easy1auth.oauth2;

import com.easy1auth.application.service.ApplicationService;
import com.easy1auth.application.model.OAuthApplicationEntity;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.*;
import org.springframework.security.oauth2.server.authorization.settings.*;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * 注册客户端仓储：将管理端配置的 OAuth 应用（{@link OAuthApplicationEntity}）
 * 动态映射为 Spring Authorization Server 的 {@link RegisteredClient}。
 *
 * <p>客户端（应用）由管理端后台统一维护，本仓储为只读，不支持写入；
 * 所有查询均限定在当前租户（{@link TenantIssuerContext}）内。</p>
 */
@Component
public class TenantRegisteredClientRepository implements RegisteredClientRepository {
    /** 应用服务（用于按租户与客户端 ID 查询 OAuth 应用） */
    private final ApplicationService applications;

    public TenantRegisteredClientRepository(ApplicationService applications) {
        this.applications = applications;
    }

    /** 不支持写入：OAuth 客户端由管理端后台（admin-api）统一维护。 */
    @Override
    public void save(RegisteredClient registeredClient) {
        throw new UnsupportedOperationException("OAuth clients are managed by admin-api");
    }

    /** 按主键查询当前租户下激活的客户端，不存在或 ID 非法时返回 null。 */
    @Override
    public RegisteredClient findById(String id) {
        try {
            return mapped(applications.findActive(TenantIssuerContext.tenantId(), UUID.fromString(id)));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** 按 client_id 查询客户端，仅返回属于当前租户的激活应用，否则返回 null。 */
    @Override
    public RegisteredClient findByClientId(String clientId) {
        var app = applications.findActiveByClientId(clientId);
        return app != null && app.tenantId().equals(TenantIssuerContext.tenantId()) ? mapped(app) : null;
    }

    /** 将 OAuth 应用实体映射为 RegisteredClient（含认证方式、授权类型、回调地址与令牌有效期等）。 */
    private static RegisteredClient mapped(OAuthApplicationEntity app) {
        if (app == null) {
            return null;
        }
        var builder = RegisteredClient.withId(app.id().toString()).clientId(app.clientId()).clientName(app.name());
        boolean publicClient = "spa".equals(app.type()) || "native".equals(app.type());
        if (publicClient) {
            builder.clientAuthenticationMethod(ClientAuthenticationMethod.NONE);
        } else {
            builder.clientSecret(app.clientSecretHash()).clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC).clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST);
        }
        app.allowedGrantTypes().forEach(value -> builder.authorizationGrantType(new AuthorizationGrantType(value)));
        app.redirectUris().forEach(builder::redirectUri);
        app.postLogoutRedirectUris().forEach(builder::postLogoutRedirectUri);
        app.scopes().forEach(builder::scope);
        if (app.allowedGrantTypes().contains("authorization_code") && !app.scopes().contains(OidcScopes.OPENID)) {
            builder.scope(OidcScopes.OPENID);
        }
        return builder.clientSettings(ClientSettings.builder().requireProofKey(app.requirePkce()).requireAuthorizationConsent(app.requireConsent()).build())
                .tokenSettings(TokenSettings.builder().accessTokenTimeToLive(Duration.ofSeconds(app.accessTokenLifetime())).refreshTokenTimeToLive(Duration.ofSeconds(app.refreshTokenLifetime())).reuseRefreshTokens(false).build()).build();
    }
}
