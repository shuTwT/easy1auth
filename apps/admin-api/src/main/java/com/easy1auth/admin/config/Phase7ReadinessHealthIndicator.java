package com.easy1auth.admin.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component("phase7Readiness")
public final class Phase7ReadinessHealthIndicator implements HealthIndicator {
    private final JdbcClient db;
    private final AdminJwtProperties jwt;

    Phase7ReadinessHealthIndicator(JdbcClient db, AdminJwtProperties jwt) {
        this.db = db;
        this.jwt = jwt;
    }

    @Override
    public Health health() {
        try {
            Integer connected = db.sql("select 1").query(Integer.class).single();
            Boolean v1 = db.sql("select exists(select 1 from flyway_schema_history where version='1' and success=true)")
                    .query(Boolean.class).single();
            URI issuer = URI.create(jwt.issuer());
            if (connected == null || connected != 1 || !Boolean.TRUE.equals(v1)
                    || issuer.getScheme() == null || issuer.getHost() == null
                    || jwt.audience() == null || jwt.audience().isBlank()
                    || jwt.secret() == null || jwt.secret().getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
                return Health.down().withDetail("schema", "Flyway V1 or required configuration unavailable").build();
            }
            return Health.up().withDetail("schema", "V1").build();
        } catch (RuntimeException ex) {
            return Health.down(ex).build();
        }
    }
}
