package com.easy1auth.authorization.config;

import com.easy1auth.tenant.WebFramework;

import com.easy1auth.directory.*;
import com.easy1auth.oauth2.*;
import com.easy1auth.useraccess.UserAccessCatalogService;
import com.easy1auth.security.SecurityPolicyService;
import com.easy1auth.authorization.security.IssuerHostValidationFilter;
import com.easy1auth.authorization.security.TenantPrincipalValidationFilter;
import com.easy1auth.authorization.web.AuthorizationInteractionService;
import com.easy1auth.authorization.web.ConsentInteractionFilter;
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
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.*;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;
import org.springframework.http.MediaType;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import java.nio.charset.StandardCharsets;

import java.util.*;

@Configuration
public class SecurityConfiguration {
    @Bean
    ConsentInteractionFilter consentInteractionFilter(AuthorizationInteractionService interactions) {
        return new ConsentInteractionFilter(interactions);
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    FilterRegistrationBean<IssuerHostValidationFilter> issuerHostValidationFilterRegistration(IssuerHostValidationFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    FilterRegistrationBean<TenantPrincipalValidationFilter> tenantPrincipalValidationFilterRegistration(TenantPrincipalValidationFilter filter) {
        var registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    AuthenticationProvider poolUserAuthenticationProvider(PoolUserAuthenticationService users, SecurityPolicyService security, com.easy1auth.security.PoolUserDeviceService devices) {
        return new AuthenticationProvider() {
            @Override
            public Authentication authenticate(Authentication authentication) throws AuthenticationException {
                PoolLoginDetails details = authentication.getDetails() instanceof PoolLoginDetails value ? value : null;
                String tenant = details == null ? null : details.tenant();
                try {
                    var principal = users.authenticate(UUID.fromString(tenant), authentication.getName(), String.valueOf(authentication.getCredentials()));
                    if (principal == null) throw new BadCredentialsException("用户名或密码错误");
                    devices.seen(principal.tenantId(), principal.id(), details.userAgent(), details.ip());
                    var authorities = new ArrayList<GrantedAuthority>();
                    authorities.add(new SimpleGrantedAuthority("ROLE_POOL_USER"));
                    authorities.add(new SimpleGrantedAuthority("TENANT_" + principal.tenantId()));
                    var mfa = security.status("pool_user", principal.id());
                    if (mfa.enabled() || security.policy(principal.tenantId()).mfaRequired())
                        authorities.add(new SimpleGrantedAuthority("MFA_REQUIRED"));
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

    @Bean
    @Order(1)
    SecurityFilterChain authorizationServerSecurity(HttpSecurity http, RegisteredClientRepository clients, OAuth2AuthorizationService authorizations, OAuth2AuthorizationConsentService consents, IssuerHostValidationFilter issuerHostValidation, TenantPrincipalValidationFilter tenantPrincipalValidation, ConsentInteractionFilter consentInteractionFilter, AuthorizationInteractionService interactions) throws Exception {
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
                // Authorization Server 在 http.build() 时才注册授权端点过滤器，不能在这里
                // 直接以 OAuth2AuthorizationEndpointFilter 作为 addFilterBefore 的定位目标。
                // 租户校验之后、授权端点处理之前执行即可完成 consent 请求的服务端重建。
                .addFilterAfter(consentInteractionFilter, TenantPrincipalValidationFilter.class)
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
            if (description == null || description.isBlank()) description = exception.getMessage();
            if (description == null || description.isBlank()) description = "OAuth2 授权请求无效";
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.getWriter().write("{\"error\":\"" + json(code) + "\",\"error_description\":\""
                    + json(description) + "\",\"path\":\"" + json(request.getRequestURI()) + "\"}");
        };
    }

    private static String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n");
    }

    @Bean
    @Order(2)
    SecurityFilterChain applicationSecurity(HttpSecurity http, AuthenticationProvider provider) throws Exception {
        return http.authenticationProvider(provider)
                .authorizeHttpRequests(auth -> auth.requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info", "/livez", "/readyz", "/auth-portal-api/**", "/oauth-login", "/oauth-login/mfa", "/oauth-consent/start", "/t/*/social/**", "/error").permitAll().anyRequest().authenticated())
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))
                .build();
    }

    @Bean
    AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().multipleIssuersAllowed(true).authorizationEndpoint("/oauth2/authorize").tokenEndpoint("/oauth2/token").tokenRevocationEndpoint("/oauth2/revoke").jwkSetEndpoint("/oauth2/jwks").oidcUserInfoEndpoint("/userinfo").oidcLogoutEndpoint("/connect/logout").build();
    }

    @Bean
    JWKSource<SecurityContext> jwkSource(TenantSigningKeyService keys) {
        return (selector, context) -> selector.select(new JWKSet(keys.active(TenantIssuerContext.tenantId())));
    }

    @Bean
    JwtDecoder jwtDecoder(JWKSource<SecurityContext> source) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(source);
    }

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
                if (user.email() != null)
                    context.getClaims().claim("email", user.email()).claim("email_verified", user.emailVerified());
                if (user.phone() != null)
                    context.getClaims().claim("phone_number", user.phone()).claim("phone_number_verified", user.phoneVerified());
                if (user.avatar() != null) context.getClaims().claim("picture", user.avatar());
                if (user.department() != null) context.getClaims().claim("department", user.department());
                if (user.position() != null) context.getClaims().claim("position", user.position());
                context.getClaims().claim("roles", new ArrayList<>(access.rolesForUser(tenant, userId).stream().map(UserAccessCatalogService.RoleView::code).toList())).claim("groups", new ArrayList<>(directory.groupsForUser(tenant, userId).stream().map(DirectoryCatalogService.GroupView::name).toList()));
            } catch (IllegalArgumentException ignored) {
            }
        };
    }

    public record PoolLoginDetails(String tenant, String userAgent, String ip) {
    }
}
