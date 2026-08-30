package com.easy1auth.oauth2;

import com.easy1auth.infrastructure.foundation.error.DomainException;
import com.easy1auth.oauth2.constant.ErrorCodeConstants;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;

import java.net.URI;
import java.util.UUID;

/**
 * 租户上下文解析工具：从当前授权服务器的 issuer URL 中解析出租户 ID。
 *
 * <p>issuer 约定形如 {@code https://auth.example.com/t/{tenantId}}，
 * 通过 {@link AuthorizationServerContextHolder} 获取当前请求的 issuer。</p>
 */
public final class TenantIssuerContext {
    private TenantIssuerContext() {
    }

    /** 从当前授权服务器上下文解析租户 ID，上下文或 issuer 缺失时抛出领域异常。 */
    public static UUID tenantId() {
        var context = AuthorizationServerContextHolder.getContext();
        if (context == null || context.getIssuer() == null) {
            throw new DomainException(ErrorCodeConstants.OAUTH_TENANT_REQUIRED);
        }
        return tenantId(context.getIssuer());
    }

    /**
     * 从 issuer URL 解析租户 ID。
     *
     * @param issuer 授权服务器 issuer（形如 .../t/{tenantId}）
     * @return 解析出的租户 ID
     */
    public static UUID tenantId(String issuer) {
        String path = URI.create(issuer).getPath();
        String[] parts = path.split("/");
        if (parts.length < 3 || !"t".equals(parts[parts.length - 2])) {
            throw new DomainException(ErrorCodeConstants.OAUTH_ISSUER_INVALID_FORMAT);
        }
        try {
            return UUID.fromString(parts[parts.length - 1]);
        } catch (IllegalArgumentException ex) {
            throw new DomainException(ErrorCodeConstants.OAUTH_ISSUER_INVALID_TENANT);
        }
    }
}
