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

/**
 * 授权门户接口控制器。
 *
 * <p>该控制器负责处理 pool_user 的密码登录、多因素认证、OAuth 授权确认，
 * 以及第三方身份源联邦登录。登录过程中的租户、授权请求和 MFA 挑战均保存在
 * 服务端 Session 中；认证成功后再将 {@link Authentication} 写入 Spring Security
 * 上下文和 HTTP Session，供后续 OAuth 请求使用。</p>
 */
@RestController
public class LoginController {
    /** 负责签发和校验 pool_user 的 TOTP 多因素认证挑战。 */
    private final SecurityPolicyService security;
    /** 负责发起和处理第三方身份源的联邦登录。 */
    private final FederationService federation;
    /** 用于校验 pool_user 用户名和密码的认证提供者。 */
    private final AuthenticationProvider poolUsers;
    /** 保存并消费登录/授权交互状态，同时提供门户页面所需上下文。 */
    private final AuthorizationInteractionService interactions;
    /** 将认证后的 SecurityContext 持久化到 HTTP Session。 */
    private final HttpSessionSecurityContextRepository securityContexts = new HttpSessionSecurityContextRepository();
    /** 保存 OAuth 授权请求，以便登录完成后继续原始请求。 */
    private final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();

    /** 创建授权门户控制器及其依赖服务。 */
    LoginController(SecurityPolicyService security, FederationService federation, AuthenticationProvider poolUsers,
                    AuthorizationInteractionService interactions) {
        this.security = security;
        this.federation = federation;
        this.poolUsers = poolUsers;
        this.interactions = interactions;
    }

    /**
     * 获取当前授权交互的门户上下文。
     *
     * <p>如果 Session 中已经存在待处理的 consent 请求，则返回授权确认上下文；
     * 否则返回登录上下文。上下文中包含租户、登录样式、错误状态和 CSRF Token 等
     * 前端渲染所需信息。</p>
     */
    @GetMapping(value = "/auth-portal-api/interaction", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> interaction(HttpServletRequest request) {
        if (request.getSession(false) != null && request.getSession(false).getAttribute(AuthorizationInteractionService.CONSENT_REQUEST) != null)
            return ResponseEntity.ok(interactions.consentContext(request));
        return ResponseEntity.ok(interactions.loginContext(request));
    }

    /**
     * 使用用户名和密码登录 pool_user。
     *
     * <p>认证成功后可能直接建立会话，也可能因为用户启用了 MFA 而返回短期挑战；
     * 认证失败只返回通用错误，避免泄露用户名是否存在。</p>
     */
    @PostMapping(value = "/auth-portal-api/login", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> passwordLogin(@RequestBody LoginInput input, HttpServletRequest request, HttpServletResponse response) {
        UUID tenant = interactions.requireTenant(request);
        if (input == null || blank(input.username()) || input.password() == null)
            return ResponseEntity.ok(new ApiError(ErrorCodeConstants.LOGIN_INPUT_INVALID.code(), ErrorCodeConstants.LOGIN_INPUT_INVALID.message()));
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
            return ResponseEntity.ok(new ApiError(ErrorCodeConstants.LOGIN_FAILED.code(), ErrorCodeConstants.LOGIN_FAILED.message()));
        }
    }

    /**
     * 校验 MFA 动态验证码并完成登录。
     *
     * <p>挑战、用户和租户必须同时存在于同一个 Session 中，且验证码只能被消费一次；
     * 校验成功后清理挑战相关 Session 属性，防止重复提交。</p>
     */
    @PostMapping(value = "/auth-portal-api/mfa", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> verifyMfa(@RequestBody MfaInput input, HttpServletRequest request, HttpServletResponse response) {
        if (input == null || blank(input.code())) return ResponseEntity.ok(new ApiError(ErrorCodeConstants.MFA_INPUT_INVALID.code(), ErrorCodeConstants.MFA_INPUT_INVALID.message()));
        var session = request.getSession(false);
        if (session == null) return ResponseEntity.ok(new ApiError(ErrorCodeConstants.MFA_EXPIRED.code(), ErrorCodeConstants.MFA_EXPIRED.message()));
        String challenge = (String) session.getAttribute("EASY1AUTH_MFA_CHALLENGE");
        UUID expected = uuid(session.getAttribute("EASY1AUTH_MFA_USER"));
        UUID tenant = uuid(session.getAttribute("EASY1AUTH_MFA_TENANT"));
        if (challenge == null || expected == null || tenant == null) return ResponseEntity.ok(new ApiError(ErrorCodeConstants.MFA_EXPIRED.code(), ErrorCodeConstants.MFA_EXPIRED.message()));
        try {
            UUID actual = security.consumeTotpChallenge(challenge, input.code(), "pool_user", "oidc_login");
            if (!actual.equals(expected)) return ResponseEntity.ok(new ApiError(ErrorCodeConstants.MFA_FAILED.code(), ErrorCodeConstants.MFA_FAILED.message()));
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_POOL_USER"), new SimpleGrantedAuthority("TENANT_" + tenant), new SimpleGrantedAuthority("MFA_VERIFIED"));
            var auth = UsernamePasswordAuthenticationToken.authenticated(org.springframework.security.core.userdetails.User.withUsername(actual.toString()).password("").authorities(authorities).build(), null, authorities);
            saveAuthentication(auth, request, response);
            session.removeAttribute("EASY1AUTH_MFA_CHALLENGE");
            session.removeAttribute("EASY1AUTH_MFA_USER");
            session.removeAttribute("EASY1AUTH_MFA_TENANT");
            interactions.clearLoginError(request);
            return ResponseEntity.ok(new LoginResult("success", continueUrl(request, response), 0));
        } catch (RuntimeException ex) {
            return ResponseEntity.ok(new ApiError(ErrorCodeConstants.MFA_FAILED.code(), ErrorCodeConstants.MFA_FAILED.message()));
        }
    }

    /**
     * 捕获当前 OAuth 授权请求并跳转到授权确认页。
     *
     * <p>授权参数由 {@link AuthorizationInteractionService} 保存在服务端，浏览器只携带
     * 不透明的交互 ID，避免用户修改回调地址、state 等关键参数。</p>
     */
    @GetMapping(value = "/oauth-consent/start", produces = MediaType.TEXT_HTML_VALUE)
    ResponseEntity<Void> consentStart(HttpServletRequest request, HttpServletResponse response) throws IOException {
        var start = interactions.captureConsent(request, response);
        return ResponseEntity.status(302).header("Location", "/oauth-consent?i=" + URLEncoder.encode(start.interactionId(), StandardCharsets.UTF_8)).build();
    }

    /** 返回当前授权确认页所需的客户端、权限范围和样式信息。 */
    @GetMapping(value = "/auth-portal-api/consent", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> consent(HttpServletRequest request) {
        return ResponseEntity.ok(interactions.consentContext(request));
    }

    /**
     * 接收授权确认页的同意或拒绝操作，并生成 OAuth 继续地址。
     *
     * <p>实际的授权决定由交互服务绑定到服务端 Session，避免仅依赖浏览器提交的参数。</p>
     */
    @PostMapping(value = "/auth-portal-api/consent", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> consentAction(@RequestBody ConsentInput input, HttpServletRequest request) {
        if (input == null) return ResponseEntity.ok(new ApiError(ErrorCodeConstants.AUTH_INPUT_INVALID.code(), ErrorCodeConstants.AUTH_INPUT_INVALID.message()));
        var continuation = interactions.consumeConsent(request, input.action());
        return ResponseEntity.ok(continuation);
    }

    /** 发起指定租户和身份源的联邦登录，并将回调地址交给身份源。 */
    @GetMapping("/t/{tenant}/federation/{provider}/authorize")
    void federationStart(@PathVariable UUID tenant, @PathVariable UUID provider, HttpServletRequest request, HttpServletResponse response) throws IOException {
        UUID interactionTenant = interactions.requireTenant(request);
        if (!tenant.equals(interactionTenant)) throw new com.easy1auth.foundation.error.DomainException(ErrorCodeConstants.AUTH_INTERACTION_MISMATCH_CLIENT_TENANT);
        String authorizePath = request.getRequestURL().toString();
        String callback = authorizePath.endsWith("/authorize")
                ? authorizePath.substring(0, authorizePath.length() - "/authorize".length()) + "/callback"
                : authorizePath + "/callback";
        response.sendRedirect(federation.authorize(tenant, provider, callback).authorizeUrl());
    }

    /**
     * 处理联邦身份源回调，建立 pool_user 会话后继续原始 OAuth 请求。
     *
     * <p>租户一致性和 code/state 的校验由联邦服务负责；控制器只负责将返回的用户
     * 映射为当前授权服务器使用的 Spring Security 身份。</p>
     */
    @GetMapping("/t/{tenant}/federation/{provider}/callback")
    void federationCallback(@PathVariable UUID tenant, @PathVariable UUID provider, @RequestParam String code, @RequestParam String state, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String callback = request.getRequestURL().toString();
        var result = federation.callback(tenant, provider, code, state, callback);
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_POOL_USER"), new SimpleGrantedAuthority("TENANT_" + tenant));
        var auth = UsernamePasswordAuthenticationToken.authenticated(org.springframework.security.core.userdetails.User.withUsername(result.poolUserId().toString()).password("").authorities(authorities).build(), null, authorities);
        saveAuthentication(auth, request, response);
        response.sendRedirect(continueUrl(request, response));
    }

    /** 将认证结果写入当前请求和 HTTP Session，供后续请求复用。 */
    private void saveAuthentication(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContexts.saveContext(context, request, response);
    }

    /**
     * 取得登录前被 Spring Security 保存的请求地址。
     *
     * <p>读取后立即移除，避免同一个 saved request 被重复消费；不存在时回到门户根路径。</p>
     */
    private String continueUrl(HttpServletRequest request, HttpServletResponse response) {
        SavedRequest saved = requestCache.getRequest(request, response);
        if (saved == null) return "/";
        requestCache.removeRequest(request, response);
        return saved.getRedirectUrl();
    }

    /** 获取并限制 User-Agent 长度，避免将异常长的客户端标识写入认证详情。 */
    private static String userAgent(HttpServletRequest request) {
        String value = request.getHeader("User-Agent");
        return value == null ? "unknown" : value.substring(0, Math.min(value.length(), 512));
    }

    /** 从 Session 属性中安全解析 UUID；属性非法时按不存在处理。 */
    private static UUID uuid(Object value) { try { return UUID.fromString(Objects.toString(value)); } catch (RuntimeException ex) { return null; } }
    /** 判断字符串是否为空或仅包含空白字符。 */
    private static boolean blank(String value) { return value == null || value.isBlank(); }

    /** 密码登录请求体。 */
    public record LoginInput(String username, String password) { }
    /** MFA 验证请求体。 */
    public record MfaInput(String code) { }
    /** 授权确认操作请求体，action 取值为 approve 或 deny。 */
    public record ConsentInput(String action) { }
    /** 登录结果；MFA 阶段返回剩余有效秒数，成功时返回继续地址。 */
    public record LoginResult(String status, String redirectUrl, int expiresIn) { }
    /** 门户接口统一错误响应。 */
    public record ApiError(int code, String message) { }
}
