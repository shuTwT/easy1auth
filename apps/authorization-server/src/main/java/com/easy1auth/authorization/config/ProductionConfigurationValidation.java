package com.easy1auth.authorization.config;

import com.easy1auth.foundation.security.SecretPolicy;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;

/**
 * 生产环境启动配置校验（仅 {@code production} 环境生效）。
 *
 * <p>启动时检查数据库密码、OAuth2 密钥加密密钥、数据加密密钥与 issuer 等关键配置
 * 是否满足安全要求，不满足则抛异常拒绝启动，防止带着弱配置上线。</p>
 */
@Component
@Profile("production")
public final class ProductionConfigurationValidation implements ApplicationRunner {
    /** 运行环境配置，用于读取环境变量类配置项 */
    private final Environment environment;
    /** OAuth2 的 issuer 基础地址，要求为无路径的 HTTPS 源 */
    private final String issuerBase;
    /** OAuth2 密钥加密密钥，用于加密存储客户端签名密钥 */
    private final String signingKeySecret;

    ProductionConfigurationValidation(Environment environment,
                                      @Value("${easy1auth.oauth2.issuer-base}") String issuerBase,
                                      @Value("${easy1auth.oauth2.key-encryption-secret:}") String signingKeySecret) {
        this.environment = environment;
        this.issuerBase = issuerBase;
        this.signingKeySecret = signingKeySecret;
    }

    /** 执行全部生产配置校验，任一不满足即抛出异常终止启动。 */
    @Override
    public void run(ApplicationArguments args) {
        SecretPolicy.require("DATABASE_PASSWORD", environment.getProperty("spring.datasource.password"), 16);
        SecretPolicy.require("OAUTH2_KEY_ENCRYPTION_SECRET", signingKeySecret, 32);
        SecretPolicy.require("SECURITY_DATA_ENCRYPTION_SECRET",
                environment.getProperty("easy1auth.security.data-encryption-secret"), 32);
        URI uri = URI.create(issuerBase);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                || (uri.getPath() != null && !uri.getPath().isBlank() && !"/".equals(uri.getPath()))
                || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalStateException("OAUTH2_ISSUER_BASE must be an HTTPS origin without path, query, or fragment");
        }
    }
}
