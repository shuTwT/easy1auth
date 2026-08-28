package com.easy1auth.admin.config;

import com.easy1auth.foundation.security.SecretPolicy;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
@Profile("production")
public final class ProductionConfigurationValidation implements ApplicationRunner {
    private final Environment environment;
    private final AdminJwtProperties jwt;
    private final RegistrationProperties registration;

    ProductionConfigurationValidation(Environment environment, AdminJwtProperties jwt,
                                      RegistrationProperties registration) {
        this.environment = environment;
        this.jwt = jwt;
        this.registration = registration;
    }

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

    private static void requireHttpsOrigin(String name, String value) {
        URI uri = URI.create(value);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                || (uri.getPath() != null && !uri.getPath().isBlank() && !"/".equals(uri.getPath()))
                || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalStateException(name + " must be an HTTPS origin without path, query, or fragment");
        }
    }
}
