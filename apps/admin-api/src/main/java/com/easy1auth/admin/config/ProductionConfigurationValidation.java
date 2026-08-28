package com.easy1auth.admin.config;

import com.easy1auth.infrastructure.foundation.security.SecretPolicy;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;

/**
 * 生产环境启动配置校验（仅 {@code production} 环境生效）。
 *
 * <p>启动时检查数据库密码、JWT 签名密钥、数据加密密钥、邮件凭据、注册发件人地址与
 * issuer 等关键配置是否满足安全要求，不满足则抛异常拒绝启动，防止带着弱配置上线。</p>
 */
@Component
@Profile("production")
public final class ProductionConfigurationValidation implements ApplicationRunner {
    /** 运行环境配置，用于读取环境变量类配置项 */
    private final Environment environment;
    /** 管理端 JWT 配置，用于校验签名密钥强度与 issuer */
    private final AdminJwtProperties jwt;
    /** 注册配置，用于校验发件人地址 */
    private final RegistrationProperties registration;

    ProductionConfigurationValidation(Environment environment, AdminJwtProperties jwt,
                                      RegistrationProperties registration) {
        this.environment = environment;
        this.jwt = jwt;
        this.registration = registration;
    }

    /** 执行全部生产配置校验，任一不满足即抛出异常终止启动。 */
    @Override
    public void run(ApplicationArguments args) {
        SecretPolicy.require("DATABASE_PASSWORD", environment.getProperty("spring.datasource.password"), 16);
        SecretPolicy.require("ADMIN_JWT_SECRET", jwt.secret(), 32);
        SecretPolicy.require("SECURITY_DATA_ENCRYPTION_SECRET",
                environment.getProperty("easy1auth.security.data-encryption-secret"), 32);
        String mailUser = environment.getProperty("spring.mail.username");
        if (mailUser == null || mailUser.isBlank()) {
            throw new IllegalStateException("MAIL_USERNAME is required");
        }
        SecretPolicy.require("MAIL_PASSWORD", environment.getProperty("spring.mail.password"), 12);
        if (registration.fromAddress() == null || registration.fromAddress().length() > 320
                || !registration.fromAddress().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalStateException("REGISTRATION_FROM_ADDRESS must be a valid email address");
        }
        requireHttpsOrigin("ADMIN_JWT_ISSUER", jwt.issuer());
    }

    /** 校验指定配置值是否为不含路径、查询串与片段的标准 HTTPS 源。 */
    private static void requireHttpsOrigin(String name, String value) {
        URI uri = URI.create(value);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                || (uri.getPath() != null && !uri.getPath().isBlank() && !"/".equals(uri.getPath()))
                || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalStateException(name + " must be an HTTPS origin without path, query, or fragment");
        }
    }
}
