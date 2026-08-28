package com.easy1auth.admin.config;

import com.easy1auth.adminidentity.AdminIdentityService;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import com.easy1auth.admin.security.TenantContextFilter;
import com.easy1auth.admin.security.TenantSecurityFilter;
import com.easy1auth.admin.security.AuditMutationFilter;
import com.easy1auth.admin.security.ApiErrorWriter;

@Configuration
@EnableConfigurationProperties({AdminJwtProperties.class, RegistrationProperties.class})
public class SecurityConfiguration {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    JwtEncoder jwtEncoder(AdminJwtProperties properties) {
        return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(key(properties)));
    }

    @Bean
    JwtDecoder jwtDecoder(AdminJwtProperties properties, AdminIdentityService identities) {
        var decoder = NimbusJwtDecoder.withSecretKey(key(properties)).macAlgorithm(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build();
        OAuth2TokenValidator<Jwt> issuer = JwtValidators.createDefaultWithIssuer(properties.issuer());
        OAuth2TokenValidator<Jwt> application = jwt -> {
            if (!jwt.getAudience().contains(properties.audience()) || !"admin".equals(jwt.getClaimAsString("subject_type"))) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Wrong admin token audience or subject type", null));
            }
            try {
                identities.validateTokenSubject(java.util.UUID.fromString(jwt.getSubject()), jwt.getClaim("security_version"));
                return OAuth2TokenValidatorResult.success();
            } catch (RuntimeException ex) {
                return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Admin session invalid", null));
            }
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuer, application));
        return decoder;
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http, TenantContextFilter tenantContextFilter, TenantSecurityFilter tenantSecurityFilter,
                                 AuditMutationFilter auditMutationFilter, ApiErrorWriter errors) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info", "/livez", "/readyz", "/api/auth/login", "/api/auth/mfa/verify", "/api/auth/register", "/api/auth/send-code", "/api/auth/refresh", "/api/login-style/public", "/api/enterprise-identity-sources/*/feishu/events").permitAll().anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions.accessDeniedHandler((request, response, exception) -> errors.writeTransport(response, 403, com.easy1auth.foundation.error.ErrorCodeConstants.ACCESS_DENIED)))
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> {
                }).authenticationEntryPoint((request, response, exception) -> errors.writeTransport(response, 401, com.easy1auth.foundation.error.ErrorCodeConstants.AUTHENTICATION_REQUIRED)))
                .addFilterAfter(tenantContextFilter, BearerTokenAuthenticationFilter.class)
                .addFilterAfter(tenantSecurityFilter, TenantContextFilter.class)
                .addFilterAfter(auditMutationFilter, TenantSecurityFilter.class)
                .build();
    }

    private static SecretKeySpec key(AdminJwtProperties properties) {
        if (properties.secret() == null || properties.secret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("ADMIN_JWT_SECRET must contain at least 32 UTF-8 bytes");
        }
        return new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
