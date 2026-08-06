package com.easy1auth.authorization.web;

import com.easy1auth.federation.FederationService;
import com.easy1auth.security.SecurityPolicyService;
import com.easy1auth.authorization.config.SecurityConfiguration;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
public class LoginController {
    private final SecurityPolicyService security;
    private final FederationService federation;
    private final AuthenticationProvider poolUsers;
    private final AuthorizationInteractionService interactions;
    private final HttpSessionSecurityContextRepository securityContexts = new HttpSessionSecurityContextRepository();
    private final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();

    LoginController(SecurityPolicyService security, FederationService federation, AuthenticationProvider poolUsers,
                    AuthorizationInteractionService interactions) {
        this.security = security;
        this.federation = federation;
        this.poolUsers = poolUsers;
        this.interactions = interactions;
    }

    @GetMapping(value = "/auth-portal-api/interaction", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> interaction(HttpServletRequest request) {
        if (request.getSession(false) != null && request.getSession(false).getAttribute(AuthorizationInteractionService.CONSENT_REQUEST) != null)
            return ResponseEntity.ok(interactions.consentContext(request));
        return ResponseEntity.ok(interactions.loginContext(request));
    }

    @PostMapping(value = "/auth-portal-api/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> passwordLogin(@RequestBody LoginInput input, HttpServletRequest request, HttpServletResponse response) {
        UUID tenant = interactions.requireTenant(request);
        if (input == null || blank(input.username()) || input.password() == null)
            return ResponseEntity.badRequest().body(new ApiError("LOGIN_INPUT_INVALID", "请输入用户名和密码"));
        var token = UsernamePasswordAuthenticationToken.unauthenticated(input.username(), input.password());
        token.setDetails(new SecurityConfiguration.PoolLoginDetails(tenant.toString(), userAgent(request), request.getRemoteAddr()));
        try {
            Authentication authentication = poolUsers.authenticate(token);
            if (authentication.getAuthorities().stream().anyMatch(a -> "MFA_REQUIRED".equals(a.getAuthority()))) {
                UUID user = UUID.fromString(authentication.getName());
                var challenge = security.issueTotpChallenge("pool_user", user, tenant, "oidc_login");
                request.getSession(true).setAttribute("EASY1AUTH_MFA_CHALLENGE", challenge.token());
                request.getSession().setAttribute("EASY1AUTH_MFA_USER", user);
                request.getSession().setAttribute("EASY1AUTH_MFA_TENANT", tenant);
                SecurityContextHolder.clearContext();
                return ResponseEntity.ok(new LoginResult("mfa_required", null, challenge.expiresIn()));
            }
            saveAuthentication(authentication, request, response);
            interactions.clearLoginError(request);
            return ResponseEntity.ok(new LoginResult("success", continueUrl(request, response), 0));
        } catch (AuthenticationException ex) {
            interactions.setLoginError(request);
            return ResponseEntity.status(401).body(new ApiError("LOGIN_FAILED", "用户名或密码错误"));
        }
    }

    @PostMapping(value = "/auth-portal-api/mfa", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> verifyMfa(@RequestBody MfaInput input, HttpServletRequest request, HttpServletResponse response) {
        if (input == null || blank(input.code())) return ResponseEntity.badRequest().body(new ApiError("MFA_INPUT_INVALID", "请输入动态验证码"));
        var session = request.getSession(false);
        if (session == null) return ResponseEntity.status(410).body(new ApiError("MFA_EXPIRED", "多因素认证已过期，请重新登录"));
        String challenge = (String) session.getAttribute("EASY1AUTH_MFA_CHALLENGE");
        UUID expected = uuid(session.getAttribute("EASY1AUTH_MFA_USER"));
        UUID tenant = uuid(session.getAttribute("EASY1AUTH_MFA_TENANT"));
        if (challenge == null || expected == null || tenant == null) return ResponseEntity.status(410).body(new ApiError("MFA_EXPIRED", "多因素认证已过期，请重新登录"));
        try {
            UUID actual = security.consumeTotpChallenge(challenge, input.code(), "pool_user", "oidc_login");
            if (!actual.equals(expected)) return ResponseEntity.status(401).body(new ApiError("MFA_FAILED", "动态验证码错误"));
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_POOL_USER"), new SimpleGrantedAuthority("TENANT_" + tenant), new SimpleGrantedAuthority("MFA_VERIFIED"));
            var auth = UsernamePasswordAuthenticationToken.authenticated(org.springframework.security.core.userdetails.User.withUsername(actual.toString()).password("").authorities(authorities).build(), null, authorities);
            saveAuthentication(auth, request, response);
            session.removeAttribute("EASY1AUTH_MFA_CHALLENGE");
            session.removeAttribute("EASY1AUTH_MFA_USER");
            session.removeAttribute("EASY1AUTH_MFA_TENANT");
            interactions.clearLoginError(request);
            return ResponseEntity.ok(new LoginResult("success", continueUrl(request, response), 0));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(401).body(new ApiError("MFA_FAILED", "动态验证码无效或已过期"));
        }
    }

    @GetMapping(value = "/oauth-consent/start", produces = MediaType.TEXT_HTML_VALUE)
    ResponseEntity<Void> consentStart(HttpServletRequest request, HttpServletResponse response) throws IOException {
        var start = interactions.captureConsent(request, response);
        return ResponseEntity.status(302).header("Location", "/oauth-consent?i=" + URLEncoder.encode(start.interactionId(), StandardCharsets.UTF_8)).build();
    }

    @GetMapping(value = "/auth-portal-api/consent", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> consent(HttpServletRequest request) {
        return ResponseEntity.ok(interactions.consentContext(request));
    }

    @PostMapping(value = "/auth-portal-api/consent", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> consentAction(@RequestBody ConsentInput input, HttpServletRequest request) {
        if (input == null) return ResponseEntity.badRequest().body(new ApiError("AUTH_INPUT_INVALID", "授权操作无效"));
        var continuation = interactions.consumeConsent(request, input.action());
        return ResponseEntity.ok(continuation);
    }

    @GetMapping("/t/{tenant}/federation/{provider}/authorize")
    void federationStart(@PathVariable UUID tenant, @PathVariable UUID provider, HttpServletRequest request, HttpServletResponse response) throws IOException {
        UUID interactionTenant = interactions.requireTenant(request);
        if (!tenant.equals(interactionTenant)) throw new com.easy1auth.foundation.error.DomainException("AUTH_INTERACTION_MISMATCH", "租户交互不匹配", 400);
        String authorizePath = request.getRequestURL().toString();
        String callback = authorizePath.endsWith("/authorize")
                ? authorizePath.substring(0, authorizePath.length() - "/authorize".length()) + "/callback"
                : authorizePath + "/callback";
        response.sendRedirect(federation.authorize(tenant, provider, callback).authorizeUrl());
    }

    @GetMapping("/t/{tenant}/federation/{provider}/callback")
    void federationCallback(@PathVariable UUID tenant, @PathVariable UUID provider, @RequestParam String code, @RequestParam String state, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String callback = request.getRequestURL().toString();
        var result = federation.callback(tenant, provider, code, state, callback);
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_POOL_USER"), new SimpleGrantedAuthority("TENANT_" + tenant));
        var auth = UsernamePasswordAuthenticationToken.authenticated(org.springframework.security.core.userdetails.User.withUsername(result.poolUserId().toString()).password("").authorities(authorities).build(), null, authorities);
        saveAuthentication(auth, request, response);
        response.sendRedirect(continueUrl(request, response));
    }

    private void saveAuthentication(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContexts.saveContext(context, request, response);
    }

    private String continueUrl(HttpServletRequest request, HttpServletResponse response) {
        SavedRequest saved = requestCache.getRequest(request, response);
        if (saved == null) return "/";
        requestCache.removeRequest(request, response);
        return saved.getRedirectUrl();
    }

    private static String userAgent(HttpServletRequest request) {
        String value = request.getHeader("User-Agent");
        return value == null ? "unknown" : value.substring(0, Math.min(value.length(), 512));
    }

    private static UUID uuid(Object value) { try { return UUID.fromString(Objects.toString(value)); } catch (RuntimeException ex) { return null; } }
    private static boolean blank(String value) { return value == null || value.isBlank(); }

    public record LoginInput(String username, String password) { }
    public record MfaInput(String code) { }
    public record ConsentInput(String action) { }
    public record LoginResult(String status, String redirectUrl, int expiresIn) { }
    public record ApiError(String code, String message) { }
}
