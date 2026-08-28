package com.easy1auth.authorization.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.net.URI;

/**
 * 阶段 7 就绪健康检查（actuator 组件名 {@code phase7Readiness}）。
 *
 * <p>对外暴露就绪状态：同时校验数据库连通性、Flyway V1 迁移是否完成，以及 issuer
 * 与 OAuth2 密钥加密密钥等关键配置是否可用，供部署时的健康探针使用。</p>
 */
@Component("phase7Readiness")
public final class Phase7ReadinessHealthIndicator implements HealthIndicator {
    /** JDBC 客户端，用于数据库连通性与迁移记录查询 */
    private final JdbcClient db;
    /** OAuth2 的 issuer 基础地址 */
    private final String issuerBase;
    /** OAuth2 密钥加密密钥，要求至少 32 字节 */
    private final String keyEncryptionSecret;

    Phase7ReadinessHealthIndicator(JdbcClient db,
                                   @Value("${easy1auth.oauth2.issuer-base}") String issuerBase,
                                   @Value("${easy1auth.oauth2.key-encryption-secret:}") String keyEncryptionSecret) {
        this.db = db;
        this.issuerBase = issuerBase;
        this.keyEncryptionSecret = keyEncryptionSecret;
    }

    /** 汇总数据库与配置的就绪状态，异常时返回 DOWN。 */
    @Override
    public Health health() {
        try {
            Integer connected = db.sql("select 1").query(Integer.class).single();
            Boolean v1 = db.sql("select exists(select 1 from flyway_schema_history where version='1' and success=true)")
                    .query(Boolean.class).single();
            URI issuer = URI.create(issuerBase);
            boolean issuerValid = issuer.getScheme() != null && issuer.getHost() != null
                    && (issuer.getPath() == null || issuer.getPath().isBlank() || "/".equals(issuer.getPath()))
                    && issuer.getQuery() == null && issuer.getFragment() == null;
            if (connected == null || connected != 1 || !Boolean.TRUE.equals(v1) || !issuerValid
                    || keyEncryptionSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
                return Health.down().withDetail("schema", "Flyway V1, issuer, or required configuration unavailable").build();
            }
            // Deliberately do not call TenantSigningKeyService: readiness must not create tenant keys.
            return Health.up().withDetail("schema", "V1").withDetail("issuerBase", issuerBase).build();
        } catch (RuntimeException ex) {
            return Health.down(ex).build();
        }
    }
}
