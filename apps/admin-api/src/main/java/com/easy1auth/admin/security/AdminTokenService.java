package com.easy1auth.admin.security;

import com.easy1auth.admin.config.AdminJwtProperties;
import com.easy1auth.adminidentity.dto.AdminAccount;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * 管理端 JWT 令牌签发服务。
 *
 * <p>为 {@link AdminAccount} 签发 HS256 签名的访问令牌，
 * 在声明中写入 subject_type=admin 与 security_version，供管理端资源服务器验签后使用。</p>
 */
@Service
public final class AdminTokenService {
    /** JWT 编码器，负责 HS256 签名 */
    private final JwtEncoder encoder;
    /** 管理端 JWT 配置，提供 issuer / audience / 有效期 */
    private final AdminJwtProperties properties;

    public AdminTokenService(JwtEncoder encoder, AdminJwtProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    /** 为指定管理账号签发访问令牌，并携带当前安全版本号。 */
    public String issue(AdminAccount account) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer(properties.issuer()).audience(List.of(properties.audience()))
                .subject(account.id().toString()).issuedAt(now).expiresAt(now.plus(properties.accessTtl()))
                .claim("subject_type", "admin").claim("security_version", account.securityVersion()).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(), claims)).getTokenValue();
    }
}
