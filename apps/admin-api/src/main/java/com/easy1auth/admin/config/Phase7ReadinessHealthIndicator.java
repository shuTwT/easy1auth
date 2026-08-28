package com.easy1auth.admin.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.net.URI;

/**
 * 阶段 7 就绪健康检查（actuator 组件名 {@code phase7Readiness}）。
 *
 * <p>对外暴露就绪状态：同时校验数据库连通性、Flyway V1 迁移是否完成，以及 JWT 相关
 * 关键配置（issuer、audience、签名密钥长度）是否可用，供部署时的健康探针使用。</p>
 */
@Component("phase7Readiness")
public final class Phase7ReadinessHealthIndicator implements HealthIndicator {
    /** JDBC 客户端，用于数据库连通性与迁移记录查询 */
    private final JdbcClient db;
    /** 管理端 JWT 配置，用于校验 issuer / audience / 签名密钥 */
    private final AdminJwtProperties jwt;

    Phase7ReadinessHealthIndicator(JdbcClient db, AdminJwtProperties jwt) {
        this.db = db;
        this.jwt = jwt;
    }

    /** 汇总数据库与配置的就绪状态，异常时返回 DOWN。 */
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
