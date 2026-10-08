package com.easy1auth.oauth2;

import com.easy1auth.framework.common.error.DomainException;
import com.easy1auth.oauth2.constant.ErrorCodeConstants;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;

import java.net.URI;
import java.util.UUID;

/**
 * 用户池上下文解析工具：从当前授权服务器的 issuer URL 中解析出用户池 ID。
 *
 * <p>issuer 约定形如 {@code https://auth.example.com/t/{userPoolId}}，
 * 通过 {@link AuthorizationServerContextHolder} 获取当前请求的 issuer。</p>
 */
public final class UserPoolIssuerContext {
    private UserPoolIssuerContext() {
    }

    /** 从当前授权服务器上下文解析用户池 ID，上下文或 issuer 缺失时抛出领域异常。 */
    public static UUID userPoolId() {
        var context = AuthorizationServerContextHolder.getContext();
        if (context == null || context.getIssuer() == null) {
            throw new DomainException(ErrorCodeConstants.OAUTH_USER_POOL_REQUIRED);
        }
        return userPoolId(context.getIssuer());
    }

    /**
     * 从 issuer URL 解析用户池 ID。
     *
     * @param issuer 授权服务器 issuer（形如 .../t/{userPoolId}）
     * @return 解析出的用户池 ID
     */
    public static UUID userPoolId(String issuer) {
        String path = URI.create(issuer).getPath();
        String[] parts = path.split("/");
        if (parts.length < 3 || !"t".equals(parts[parts.length - 2])) {
            throw new DomainException(ErrorCodeConstants.OAUTH_ISSUER_INVALID_FORMAT);
        }
        try {
            return UUID.fromString(parts[parts.length - 1]);
        } catch (IllegalArgumentException ex) {
            throw new DomainException(ErrorCodeConstants.OAUTH_ISSUER_INVALID_USER_POOL);
        }
    }
}
