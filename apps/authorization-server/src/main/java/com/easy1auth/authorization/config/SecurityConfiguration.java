package com.easy1auth.authorization.config;

import com.easy1auth.poolidentity.service.DirectoryCatalogService;

import com.easy1auth.poolidentity.service.PoolUserAuthenticationService;
import com.easy1auth.poolidentity.service.PoolUserService;
import com.easy1auth.oauth2.*;
import com.easy1auth.poolidentity.service.UserAccessCatalogService;
import com.easy1auth.security.service.SecurityPolicyService;
import com.easy1auth.authorization.security.IssuerHostValidationFilter;
import com.easy1auth.authorization.security.TenantPrincipalValidationFilter;
import com.easy1auth.authorization.web.AuthorizationInteractionService;
import com.easy1auth.security.service.PoolUserDeviceService;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.*;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.security.web.*;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.http.MediaType;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.nio.charset.StandardCharsets;

import java.util.*;

/**
 * 授权服务器安全配置。
 *
 * <p>装配两条过滤链：{@code @Order(1)} 处理 OAuth2 授权端点（含授权、令牌、吊销、
 * JWKS 与 OIDC），{@code @Order(2)} 处理登录门户与常规请求。提供 pool_user 的
 * 认证提供者、多租户 JWT 密钥源、令牌声明定制与门户跳转等能力。</p>
 */
@Configuration
public class SecurityConfiguration {
    /** 声明密码编码器：BCrypt，强度 12（用于门户与授权相关密码场景）。 */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /** 声明并默认禁用 {@link IssuerHostValidationFilter}（由授权过滤链按需启用）。 */
    @Bean
    FilterRegistrationBean<IssuerHostValidationFilter> issuerHostValidationFilterRegistration(IssuerHostValidationFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /** 声明并默认禁用 {@link TenantPrincipalValidationFilter}（由授权过滤链按需启用）。 */
    @Bean
    FilterRegistrationBean<TenantPrincipalValidationFilter> tenantPrincipalValidationFilterRegistration(TenantPrincipalValidationFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * 声明 pool_user 用户名/密码认证提供者。
     *
     * <p>从认证细节中的租户解析用户，校验通过后授予 ROLE_POOL_USER、TENANT_{租户} 权限，
     * 并按策略在需要时附加 MFA_REQUIRED，供登录流程进入多因素认证。</p>
     */
    @Bean
    AuthenticationProvider poolUserAuthenticationProvider(PoolUserAuthenticationService users, SecurityPolicyService security, PoolUserDeviceService devices) {
        return new AuthenticationProvider() {
            @Override
            public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                PoolLoginDetails details = authentication.getDetails() instanceof PoolLoginDetails value ? value : null;
                String tenant = details == null ? null : details.tenant();
                try {
                    var principal = users.authenticate(UUID.fromString(tenant), authentication.getName(), String.valueOf(authentication.getCredentials()));
                    if (principal == null) {
                        throw new BadCredentialsException("用户名或密码错误");
                    }
                    devices.seen(principal.tenantId(), principal.id(), details.userAgent(), details.ip());
                    var authorities = new ArrayList<GrantedAuthority>();
                    authorities.add(new SimpleGrantedAuthority("ROLE_POOL_USER"));
                    authorities.add(new SimpleGrantedAuthority("TENANT_" + principal.tenantId()));
                    var mfa = security.status("pool_user", principal.id());
                    if (mfa.enabled() || security.policy(principal.tenantId()).mfaRequired()) {
                        authorities.add(new SimpleGrantedAuthority("MFA_REQUIRED"));
                    }
                    return UsernamePasswordAuthenticationToken.authenticated(org.springframework.security.core.userdetails.User.withUsername(principal.id().toString()).password("").authorities(authorities).build(), null, authorities);
                } catch (IllegalArgumentException ex) {
                    throw new BadCredentialsException("租户或凭据无效");
                }
            }

            @Override
            public boolean supports(Class<?> authentication) {
                return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
            }
        };
    }

    /**
     * 声明 OAuth2 授权服务器安全过滤链（最高优先级）。
     *
     * <p>配置授权、令牌、吊销、JWKS 与 OIDC 端点；在安全上下文之后挂载 issuer 主机
     * 校验与租户主体校验过滤器；未认证的 HTML 请求跳转到登录门户并捕获原始授权请求。</p>
     */
    @Bean
    @Order(1)
    SecurityFilterChain authorizationServerSecurity(HttpSecurity http, RegisteredClientRepository clients, OAuth2AuthorizationService authorizations, OAuth2AuthorizationConsentService consents, IssuerHostValidationFilter issuerHostValidation, TenantPrincipalValidationFilter tenantPrincipalValidation, AuthorizationInteractionService interactions) throws Exception {
        var configurer = OAuth2AuthorizationServerConfigurer.authorizationServer();
        http.securityMatcher(configurer.getEndpointsMatcher())
                .with(configurer, server -> server.registeredClientRepository(clients).authorizationService(authorizations).authorizationConsentService(consents)
                        .authorizationEndpoint(endpoint -> endpoint
                                .consentPage("/oauth-consent/start")
                                .errorResponseHandler(oauthErrorResponseHandler()))
                        .oidc(Customizer.withDefaults()))
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()).ignoringRequestMatchers(configurer.getEndpointsMatcher()))
                .addFilterAfter(issuerHostValidation, SecurityContextHolderFilter.class)
                .addFilterAfter(tenantPrincipalValidation, IssuerHostValidationFilter.class)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .oauth2ResourceServer(resource -> resource.jwt(Customizer.withDefaults()))
                .exceptionHandling(errors -> errors.defaultAuthenticationEntryPointFor((request, response, exception) -> {
                    interactions.captureAuthorizationRequest(request, response);
                    response.sendRedirect("/oauth-login");
                }, new MediaTypeRequestMatcher(MediaType.TEXT_HTML)));
        return http.build();
    }

    /**
     * 返回授权请求的结构化错误，避免 redirect_uri 无效等错误进入 Spring Boot
     * 默认的 Whitelabel Error Page。这里不能盲目重定向：当 redirect_uri 本身
     * 不可信时，OAuth2 规范要求错误留在授权服务器，不能把错误发往请求方。
     */
    private static AuthenticationFailureHandler oauthErrorResponseHandler() {
        return (request, response, exception) -> {
            OAuth2Error error = exception instanceof OAuth2AuthenticationException oauth
                    ? oauth.getError() : null;
            String code = error == null || error.getErrorCode() == null ? "invalid_request" : error.getErrorCode();
            String description = error == null ? null : error.getDescription();
            if (description == null || description.isBlank()) {
                description = exception.getMessage();
            }
            if (description == null || description.isBlank()) {
                description = "OAuth2 授权请求无效";
            }
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"error\":\"" + json(code) + "\",\"error_description\":\""
                    + json(description) + "\",\"path\":\"" + json(request.getRequestURI()) + "\"}");
        };
    }

    /** JSON 转义工具：将字符串中会破坏 JSON 结构的字符替换为转义形式。 */
    private static String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n");
    }

    /**
     * 声明登录门户与常规请求的安全过滤链（次高优先级）。
     *
     * <p>放行健康检查、门户 API、登录/授权确认页与社交登录等公开端点，其余请求需
     * 认证；启用基于 Cookie 的 CSRF 保护以支撑表单提交。</p>
     */
    @Bean
    @Order(2)
    SecurityFilterChain applicationSecurity(HttpSecurity http, AuthenticationProvider provider) throws Exception {
        return http.authenticationProvider(provider)
                .authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info", "/livez", "/readyz", "/auth-portal-api/**", "/oauth-login", "/oauth-login/mfa", "/oauth-consent/start", "/t/*/social/**", "/error").permitAll().anyRequest().authenticated())
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .build();
    }

    /** 声明授权服务器端点路径，支持多 issuer 并开放完整的 OAuth2 / OIDC 端点。 */
    @Bean
    AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().multipleIssuersAllowed(true).authorizationEndpoint("/oauth2/authorize").tokenEndpoint("/oauth2/token").tokenRevocationEndpoint("/oauth2/revoke").jwkSetEndpoint("/oauth2/jwks").oidcUserInfoEndpoint("/userinfo").oidcLogoutEndpoint("/connect/logout").build();
    }

    /** 声明 JWKS 密钥源：按当前租户上下文返回该租户激活的签名密钥集。 */
    @Bean
    JWKSource<SecurityContext> jwkSource(TenantSigningKeyService keys) {
        return (selector, context) -> selector.select(new JWKSet(keys.active(TenantIssuerContext.tenantId())));
    }

    /** 声明基于多租户密钥源的 JWT 解码器。 */
    @Bean
    JwtDecoder jwtDecoder(JWKSource<SecurityContext> source) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(source);
    }

    /**
     * 声明令牌声明定制器。
     *
     * <p>为签发的访问令牌注入 tenant_id 与 subject_type；对 pool_user 主体补充用户名、
     * 邮箱、角色、组织等用户目录信息；客户端凭证模式则标记为 oauth_client 主体。</p>
     */
    @Bean
    OAuth2TokenCustomizer<JwtEncodingContext> tokenClaims(PoolUserService users, DirectoryCatalogService directory, UserAccessCatalogService access) {
        return context -> {
            UUID tenant = TenantIssuerContext.tenantId();
            context.getClaims().audience(new ArrayList<>(List.of(context.getRegisteredClient().getClientId()))).claim("tenant_id", tenant.toString());
            if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(context.getAuthorizationGrantType())) {
                context.getClaims().claim("subject_type", "oauth_client");
                return;
            }
            try {
                UUID userId = UUID.fromString(context.getPrincipal().getName());
                var user = users.get(tenant, userId);
                context.getClaims().subject(userId.toString()).claim("subject_type", "pool_user").claim("name", user.name()).claim("preferred_username", user.username());
                if (user.email() != null) {
                    context.getClaims().claim("email", user.email()).claim("email_verified", user.emailVerified());
                }
                if (user.phone() != null) {
                    context.getClaims().claim("phone_number", user.phone()).claim("phone_number_verified", user.phoneVerified());
                }
                if (user.avatar() != null) {
                    context.getClaims().claim("picture", user.avatar());
                }
                if (user.department() != null) {
                    context.getClaims().claim("department", user.department());
                }
                if (user.position() != null) {
                    context.getClaims().claim("position", user.position());
                }
                context.getClaims().claim("roles", new ArrayList<>(access.rolesForUser(tenant, userId).stream().map(UserAccessCatalogService.RoleView::code).toList())).claim("groups", new ArrayList<>(directory.groupsForUser(tenant, userId).stream().map(DirectoryCatalogService.GroupView::name).toList()));
            } catch (IllegalArgumentException ignored) {
            }
        };
    }

    /**
     * 门户登录附带信息，随认证请求传入认证提供者。
     *
     * @param tenant    登录所属租户 UUID
     * @param userAgent 用户代理（浏览器标识）
     * @param ip        客户端 IP
     */
    public record PoolLoginDetails(String tenant, String userAgent, String ip) {
    }
}
