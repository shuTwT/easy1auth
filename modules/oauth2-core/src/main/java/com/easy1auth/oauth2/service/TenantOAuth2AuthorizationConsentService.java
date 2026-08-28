package com.easy1auth.oauth2.service;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.oauth2.server.authorization.*;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Component;

/**
 * 租户隔离的 OAuth2 授权同意（consent）服务。
 *
 * <p>委托 {@link JdbcOAuth2AuthorizationConsentService} 完成 JDBC 持久化，并在每次读写前
 * 校验授权同意记录所属的客户端仍存在于当前租户内，防止跨租户访问。</p>
 */
@Component
public class TenantOAuth2AuthorizationConsentService implements OAuth2AuthorizationConsentService {
    /** JDBC 授权同意服务的委托实现 */
    private final JdbcOAuth2AuthorizationConsentService delegate;
    /** 已注册客户端仓储（用于校验客户端是否属于当前租户） */
    private final RegisteredClientRepository clients;

    public TenantOAuth2AuthorizationConsentService(JdbcOperations jdbc, RegisteredClientRepository clients) {
        this.delegate = new JdbcOAuth2AuthorizationConsentService(jdbc, clients);
        this.clients = clients;
    }

    /** 保存授权同意记录（保存前校验其客户端属于当前租户）。 */
    @Override
    public void save(OAuth2AuthorizationConsent consent) {
        requireClient(consent.getRegisteredClientId());
        delegate.save(consent);
    }

    /** 移除授权同意记录（移除前校验其客户端属于当前租户）。 */
    @Override
    public void remove(OAuth2AuthorizationConsent consent) {
        requireClient(consent.getRegisteredClientId());
        delegate.remove(consent);
    }

    /** 按客户端 ID 与主体名称查询授权同意记录，不属于当前租户时抛出异常。 */
    @Override
    public OAuth2AuthorizationConsent findById(String registeredClientId, String principalName) {
        requireClient(registeredClientId);
        return delegate.findById(registeredClientId, principalName);
    }

    /** 校验指定客户端存在于当前租户，不存在时抛出参数异常。 */
    private void requireClient(String id) {
        if (clients.findById(id) == null) {
            throw new IllegalArgumentException("OAuth consent does not belong to requested tenant");
        }
    }
}
