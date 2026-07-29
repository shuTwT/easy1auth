package com.easy1auth.authorization.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component("phase7Readiness")
public final class Phase7ReadinessHealthIndicator implements HealthIndicator {
    private final JdbcClient db;
    private final String issuerBase;
    private final String keyEncryptionSecret;

    Phase7ReadinessHealthIndicator(JdbcClient db,
            @Value("${easy1auth.oauth2.issuer-base}") String issuerBase,
            @Value("${easy1auth.oauth2.key-encryption-secret:}") String keyEncryptionSecret) {
        this.db = db;
        this.issuerBase = issuerBase;
        this.keyEncryptionSecret = keyEncryptionSecret;
    }

    @Override
    public Health health() {
        try {
            Integer connected = db.sql("select 1").query(Integer.class).single();
            Boolean v7 = db.sql("select exists(select 1 from flyway_schema_history where version='7' and success=true)")
                    .query(Boolean.class).single();
            URI issuer = URI.create(issuerBase);
            boolean issuerValid = issuer.getScheme() != null && issuer.getHost() != null
                    && (issuer.getPath() == null || issuer.getPath().isBlank() || "/".equals(issuer.getPath()))
                    && issuer.getQuery() == null && issuer.getFragment() == null;
            if (connected == null || connected != 1 || !Boolean.TRUE.equals(v7) || !issuerValid
                    || keyEncryptionSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
                return Health.down().withDetail("schema", "Flyway V7, issuer, or required configuration unavailable").build();
            }
            // Deliberately do not call TenantSigningKeyService: readiness must not create tenant keys.
            return Health.up().withDetail("schema", "V7").withDetail("issuerBase", issuerBase).build();
        } catch (RuntimeException ex) {
            return Health.down(ex).build();
        }
    }
}
