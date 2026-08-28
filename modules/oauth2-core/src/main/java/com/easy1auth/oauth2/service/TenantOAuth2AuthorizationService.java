package com.easy1auth.oauth2.service;

import com.easy1auth.oauth2.TenantIssuerContext;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.oauth2.server.authorization.*;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.stereotype.Component;

/**
 * 租户隔离的 OAuth2 授权信息服务。
 *
 * <p>委托 {@link JdbcOAuth2AuthorizationService} 完成 JDBC 持久化，并在读写前
 * 校验授权记录属于当前租户（按 tenant_id 比对），防止跨租户访问。</p>
 */
@Component
public class TenantOAuth2AuthorizationService implements OAuth2AuthorizationService {
    /** JDBC 授权信息服务的委托实现 */
    private final JdbcOAuth2AuthorizationService delegate;
    /** 已注册客户端仓储（用于校验客户端是否属于当前租户） */
    private final RegisteredClientRepository clients;
    /** JDBC 操作器（用于按 tenant_id 校验授权记录归属） */
    private final JdbcOperations jdbc;

    public TenantOAuth2AuthorizationService(JdbcOperations jdbc, RegisteredClientRepository clients) {
        this.delegate = new JdbcOAuth2AuthorizationService(jdbc, clients);
        this.clients = clients;
        this.jdbc = jdbc;
    }

    /** 保存授权记录（保存前校验其客户端属于当前租户）。 */
    @Override
    public void save(OAuth2Authorization authorization) {
        requireClient(authorization.getRegisteredClientId());
        delegate.save(authorization);
    }

    /** 移除授权记录（移除前校验其客户端属于当前租户）。 */
    @Override
    public void remove(OAuth2Authorization authorization) {
        requireClient(authorization.getRegisteredClientId());
        delegate.remove(authorization);
    }

    /** 按主键查询授权记录，仅返回属于当前租户的记录，否则返回 null。 */
    @Override
    public OAuth2Authorization findById(String id) {
        return belongs("select tenant_id from oauth2_authorization where id=?", id) ? valid(delegate.findById(id)) : null;
    }

    /** 按令牌值查询授权记录，仅返回属于当前租户的记录，否则返回 null。 */
    @Override
    public OAuth2Authorization findByToken(String token, OAuth2TokenType tokenType) {
        return belongs("select tenant_id from oauth2_authorization where state=? or authorization_code_value=? or access_token_value=? or refresh_token_value=? or oidc_id_token_value=? or user_code_value=? or device_code_value=?", token, token, token, token, token, token, token) ? valid(delegate.findByToken(token, tokenType)) : null;
    }

    /** 校验查询到的授权记录，其客户端不属于当前租户时抛出异常。 */
    private OAuth2Authorization valid(OAuth2Authorization value) {
        if (value == null) {
            return null;
        }
        requireClient(value.getRegisteredClientId());
        return value;
    }

    /** 校验指定客户端存在于当前租户，不存在时抛出参数异常。 */
    private void requireClient(String id) {
        if (clients.findById(id) == null) {
            throw new IllegalArgumentException("OAuth authorization does not belong to requested tenant");
        }
    }

    /** 判断指定 SQL 查询出的 tenant_id 均属于当前租户（空结果视为不匹配）。 */
    private boolean belongs(String statement, Object... params) {
        var tenants = jdbc.query(statement, (rs, row) -> rs.getObject(1, java.util.UUID.class), params);
        return !tenants.isEmpty() && tenants.stream().allMatch(TenantIssuerContext.tenantId()::equals);
    }
}
