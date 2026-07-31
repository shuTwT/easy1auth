package com.easy1auth.oauth2;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.oauth2.server.authorization.*;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Component;

@Component
public class TenantOAuth2AuthorizationConsentService implements OAuth2AuthorizationConsentService {
    private final JdbcOAuth2AuthorizationConsentService delegate;
    private final RegisteredClientRepository clients;

    public TenantOAuth2AuthorizationConsentService(JdbcOperations jdbc, RegisteredClientRepository clients) {
        this.delegate = new JdbcOAuth2AuthorizationConsentService(jdbc, clients);
        this.clients = clients;
    }

    @Override
    public void save(OAuth2AuthorizationConsent consent) {
        requireClient(consent.getRegisteredClientId());
        delegate.save(consent);
    }

    @Override
    public void remove(OAuth2AuthorizationConsent consent) {
        requireClient(consent.getRegisteredClientId());
        delegate.remove(consent);
    }

    @Override
    public OAuth2AuthorizationConsent findById(String registeredClientId, String principalName) {
        requireClient(registeredClientId);
        return delegate.findById(registeredClientId, principalName);
    }

    private void requireClient(String id) {
        if (clients.findById(id) == null)
            throw new IllegalArgumentException("OAuth consent does not belong to requested tenant");
    }
}
