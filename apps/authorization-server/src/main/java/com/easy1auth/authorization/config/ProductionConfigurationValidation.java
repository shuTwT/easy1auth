package com.easy1auth.authorization.config;

import com.easy1auth.foundation.security.SecretPolicy;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
@Profile("production")
public final class ProductionConfigurationValidation implements ApplicationRunner {
    private final Environment environment;
    private final String issuerBase;
    private final String signingKeySecret;

    ProductionConfigurationValidation(Environment environment,
            @Value("${easy1auth.oauth2.issuer-base}") String issuerBase,
            @Value("${easy1auth.oauth2.key-encryption-secret:}") String signingKeySecret) {
        this.environment = environment;
        this.issuerBase = issuerBase;
        this.signingKeySecret = signingKeySecret;
    }

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
