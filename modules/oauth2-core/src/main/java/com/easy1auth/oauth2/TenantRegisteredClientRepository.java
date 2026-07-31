package com.easy1auth.oauth2;

import com.easy1auth.application.ApplicationService;
import com.easy1auth.application.model.OAuthApplicationEntity;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.*;
import org.springframework.security.oauth2.server.authorization.settings.*;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class TenantRegisteredClientRepository implements RegisteredClientRepository {
    private final ApplicationService applications;

    public TenantRegisteredClientRepository(ApplicationService applications) {
        this.applications = applications;
    }

    @Override
    public void save(RegisteredClient registeredClient) {
        throw new UnsupportedOperationException("OAuth clients are managed by admin-api");
    }

    @Override
    public RegisteredClient findById(String id) {
        try {
            return mapped(applications.findActive(TenantIssuerContext.tenantId(), UUID.fromString(id)));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        var app = applications.findActiveByClientId(clientId);
        return app != null && app.tenantId().equals(TenantIssuerContext.tenantId()) ? mapped(app) : null;
    }

    private static RegisteredClient mapped(OAuthApplicationEntity app) {
        if (app == null) return null;
        var builder = RegisteredClient.withId(app.id().toString()).clientId(app.clientId()).clientName(app.name());
        boolean publicClient = "spa".equals(app.type()) || "native".equals(app.type());
        if (publicClient) builder.clientAuthenticationMethod(ClientAuthenticationMethod.NONE);
        else
            builder.clientSecret(app.clientSecretHash()).clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC).clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST);
        app.allowedGrantTypes().forEach(value -> builder.authorizationGrantType(new AuthorizationGrantType(value)));
        app.redirectUris().forEach(builder::redirectUri);
        app.postLogoutRedirectUris().forEach(builder::postLogoutRedirectUri);
        app.scopes().forEach(builder::scope);
        if (app.allowedGrantTypes().contains("authorization_code") && !app.scopes().contains(OidcScopes.OPENID))
            builder.scope(OidcScopes.OPENID);
        return builder.clientSettings(ClientSettings.builder().requireProofKey(app.requirePkce()).requireAuthorizationConsent(app.requireConsent()).build())
                .tokenSettings(TokenSettings.builder().accessTokenTimeToLive(Duration.ofSeconds(app.accessTokenLifetime())).refreshTokenTimeToLive(Duration.ofSeconds(app.refreshTokenLifetime())).reuseRefreshTokens(false).build()).build();
    }
}
