package com.easy1auth.admin.security;

import com.easy1auth.admin.config.AdminJwtProperties;
import com.easy1auth.adminidentity.AdminAccount;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;

@Service
public final class AdminTokenService {
    private final JwtEncoder encoder;
    private final AdminJwtProperties properties;
    public AdminTokenService(JwtEncoder encoder, AdminJwtProperties properties) { this.encoder = encoder; this.properties = properties; }
    public String issue(AdminAccount account) {
        Instant now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer(properties.issuer()).audience(List.of(properties.audience()))
                .subject(account.id().toString()).issuedAt(now).expiresAt(now.plus(properties.accessTtl()))
                .claim("subject_type", "admin").claim("security_version", account.securityVersion()).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(), claims)).getTokenValue();
    }
}
