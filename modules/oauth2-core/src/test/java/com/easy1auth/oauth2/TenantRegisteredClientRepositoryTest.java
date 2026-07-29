package com.easy1auth.oauth2;

import com.easy1auth.application.ApplicationService;
import com.easy1auth.application.model.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.server.authorization.context.*;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TenantRegisteredClientRepositoryTest {
    @AfterEach void reset(){AuthorizationServerContextHolder.resetContext();}

    @Test void mapsPkceRotationAndSecretWithoutCrossTenantLookup(){
        UUID tenant=UUID.randomUUID(),other=UUID.randomUUID(),id=UUID.randomUUID();ApplicationService applications=mock(ApplicationService.class);
        var entity=OAuthApplicationEntityDraft.$.produce(d->d.setId(id).setTenantId(tenant).setName("Web").setLogo(null).setDescription(null).setType("web").setClientId("client").setClientSecretHash("bcrypt-hash").setRedirectUris(List.of("https://client.example/callback")).setPostLogoutRedirectUris(List.of("https://client.example/logout")).setAllowedGrantTypes(List.of("authorization_code","refresh_token")).setScopes(List.of("openid","profile")).setRequirePkce(true).setRequireConsent(true).setAccessTokenLifetime(900).setRefreshTokenLifetime(3600).setStatus("active").setCreatedAt(Instant.now()).setUpdatedAt(Instant.now()));
        when(applications.findActiveByClientId("client")).thenReturn(entity);var repository=new TenantRegisteredClientRepository(applications);

        issuer(tenant);var client=repository.findByClientId("client");
        assertThat(client).isNotNull();assertThat(client.getClientSecret()).isEqualTo("{bcrypt}bcrypt-hash");
        assertThat(client.getAuthorizationGrantTypes()).contains(AuthorizationGrantType.AUTHORIZATION_CODE,AuthorizationGrantType.REFRESH_TOKEN);
        assertThat(client.getClientSettings().isRequireProofKey()).isTrue();assertThat(client.getTokenSettings().isReuseRefreshTokens()).isFalse();
        issuer(other);assertThat(repository.findByClientId("client")).isNull();
    }

    @Test void parsesOnlyTenantIssuerShape(){UUID tenant=UUID.randomUUID();assertThat(TenantIssuerContext.tenantId("https://auth.example/t/"+tenant)).isEqualTo(tenant);}
    private static void issuer(UUID tenant){AuthorizationServerContext context=mock(AuthorizationServerContext.class);when(context.getIssuer()).thenReturn("https://auth.example/t/"+tenant);when(context.getAuthorizationServerSettings()).thenReturn(AuthorizationServerSettings.builder().multipleIssuersAllowed(true).build());AuthorizationServerContextHolder.setContext(context);}
}
