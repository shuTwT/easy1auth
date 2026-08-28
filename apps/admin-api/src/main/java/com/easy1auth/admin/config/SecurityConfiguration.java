package com.easy1auth.admin.config;

import com.easy1auth.adminidentity.service.AdminIdentityService;
import com.easy1auth.infrastructure.foundation.error.ErrorCodeConstants;
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

/**
 * 管理端 API 安全配置。
 *
 * <p>装配管理端安全过滤链：配置公开路由白名单，其余请求要求 JWT 认证；提供 HS256
 * 的 JWT 编解码器并校验 issuer / audience / subject_type 与账号会话有效性；同时在
 * 认证过滤器之后挂载租户上下文解析、租户权限与审计三个自定义过滤器。</p>
 */
@Configuration
@EnableConfigurationProperties({AdminJwtProperties.class, RegistrationProperties.class})
public class SecurityConfiguration {
    /** 声明密码编码器：BCrypt，强度 12。 */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /** 声明 JWT 编码器，使用配置的签名密钥进行 HS256 签名。 */
    @Bean
    JwtEncoder jwtEncoder(AdminJwtProperties properties) {
        return new NimbusJwtEncoder(new ImmutableSecret<SecurityContext>(key(properties)));
    }

    /**
     * 声明 JWT 解码器：HS256 验签，并校验 issuer、audience、subject_type=admin，
     * 以及账号会话是否仍有效（security_version 未失效）。
     */
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

    /**
     * 声明管理端安全过滤链。
     *
     * <p>关闭 CSRF（无状态 JWT 认证）；放行健康检查、登录/注册/刷新等公开端点；
     * 其余请求需认证。认证通过后依次执行租户上下文解析、租户安全校验与审计过滤器。</p>
     */
    @Bean
    SecurityFilterChain security(HttpSecurity http, TenantContextFilter tenantContextFilter, TenantSecurityFilter tenantSecurityFilter,
                                 AuditMutationFilter auditMutationFilter, ApiErrorWriter errors) throws Exception {
        return http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info", "/livez", "/readyz", "/api/auth/login", "/api/auth/mfa/verify", "/api/auth/register", "/api/auth/send-code", "/api/auth/refresh", "/api/login-style/public", "/api/enterprise-identity-sources/*/feishu/events").permitAll().anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions.accessDeniedHandler((request, response, exception) -> errors.writeTransport(response, 403, ErrorCodeConstants.ACCESS_DENIED)))
                .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> {
                }).authenticationEntryPoint((request, response, exception) -> errors.writeTransport(response, 401, ErrorCodeConstants.AUTHENTICATION_REQUIRED)))
                .addFilterAfter(tenantContextFilter, BearerTokenAuthenticationFilter.class)
                .addFilterAfter(tenantSecurityFilter, TenantContextFilter.class)
                .addFilterAfter(auditMutationFilter, TenantSecurityFilter.class)
                .build();
    }

    /** 由配置的签名密钥构造 HS256 密钥规格；密钥不足 32 字节时拒绝启动。 */
    private static SecretKeySpec key(AdminJwtProperties properties) {
        if (properties.secret() == null || properties.secret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("ADMIN_JWT_SECRET must contain at least 32 UTF-8 bytes");
        }
        return new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}
