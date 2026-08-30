package com.easy1auth.authorization.web;

import com.easy1auth.authorization.constant.ErrorCodeConstants;
import com.easy1auth.customization.service.CustomizationService;
import com.easy1auth.poolidentity.service.PoolUserService;
import com.easy1auth.poolidentity.dto.PoolUserInput;
import com.easy1auth.poolidentity.model.PoolUserEntityTable;
import com.easy1auth.common.foundation.error.DomainException;
import com.easy1auth.social.service.SocialIdentityService;
import com.easy1auth.social.dto.PendingIdentity;
import com.easy1auth.security.service.SecurityPolicyService;
import com.easy1auth.security.dto.Challenge;
import com.easy1auth.security.dto.ConsumedEmailChallenge;
import com.easy1auth.authorization.config.SecurityConfiguration;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.babyfish.jimmer.sql.JSqlClient;
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
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
    /** Session 中暂存的已验证外部身份（等待绑定或创建新账号） */
    private static final String SOCIAL_PENDING_IDENTITY = "EASY1AUTH_SOCIAL_PENDING_IDENTITY";
    /** 负责签发和校验 pool_user 的 TOTP 多因素认证挑战。 */
    private final SecurityPolicyService security;
    /** 负责发起和处理社会化身份源登录。 */
    private final SocialIdentityService socialIdentity;
    /** 用于校验 pool_user 用户名和密码的认证提供者。 */
    private final AuthenticationProvider poolUsers;
    /** 保存并消费登录/授权交互状态，同时提供门户页面所需上下文。 */
    private final AuthorizationInteractionService interactions;
    /** 查询租户登录样式，用于判断是否开放自助注册。 */
    private final CustomizationService customization;
    /** 创建 pool_user，复用管理端创建逻辑完成自助注册。 */
    private final PoolUserService poolUsersService;
    /** 查询 pool_user，用于注册前邮箱占用检查和用户名冲突检查。 */
    private final JSqlClient sql;
    /** 发送注册和登录验证码邮件。 */
    private final org.springframework.mail.javamail.JavaMailSender mail;
    /** 验证码邮件的发件人地址。 */
    private final String mailFrom;
    /** 将认证后的 SecurityContext 持久化到 HTTP Session。 */
    private final HttpSessionSecurityContextRepository securityContexts = new HttpSessionSecurityContextRepository();
    /** 保存 OAuth 授权请求，以便登录完成后继续原始请求。 */
    private final HttpSessionRequestCache requestCache = new HttpSessionRequestCache();

    /** 创建授权门户控制器及其依赖服务。 */
    LoginController(SecurityPolicyService security, SocialIdentityService socialIdentity, AuthenticationProvider poolUsers,
                    AuthorizationInteractionService interactions, CustomizationService customization,
                    PoolUserService poolUsersService, JSqlClient sql,
                    org.springframework.mail.javamail.JavaMailSender mail,
                    @org.springframework.beans.factory.annotation.Value("${easy1auth.mail.from:no-reply@easy1auth.local}") String mailFrom) {
        this.security = security;
        this.socialIdentity = socialIdentity;
        this.poolUsers = poolUsers;
        this.interactions = interactions;
        this.customization = customization;
        this.poolUsersService = poolUsersService;
        this.sql = sql;
        this.mail = mail;
        this.mailFrom = mailFrom;
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
        if (request.getSession(false) != null && request.getSession(false).getAttribute(AuthorizationInteractionService.CONSENT_REQUEST) != null) {
            return ResponseEntity.ok(interactions.consentContext(request));
        }
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
        if (input == null || blank(input.username()) || input.password() == null) {
            return ResponseEntity.ok(new ApiError(ErrorCodeConstants.LOGIN_INPUT_INVALID.code(), ErrorCodeConstants.LOGIN_INPUT_INVALID.message()));
        }
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
            bindPendingSocialIdentity(request, UUID.fromString(authentication.getName()));
            saveAuthentication(authentication, request, response);
            interactions.clearLoginError(request);
            return ResponseEntity.ok(new LoginResult("success", continueUrl(request, response), 0));
        } catch (AuthenticationException ex) {
            interactions.setLoginError(request);
            return ResponseEntity.ok(new ApiError(ErrorCodeConstants.LOGIN_FAILED.code(), ErrorCodeConstants.LOGIN_FAILED.message()));
        }
    }

    /**
     * 注册第一步：校验邮箱并发送验证码。
     *
     * <p>校验租户注册开关、邮箱格式和邮箱是否已被注册。邮箱已注册时返回警告，
     * 不发送验证码；未注册则生成 6 位验证码并通过邮件发送，返回不透明的挑战
     * token 供第二步验证使用。</p>
     */
    @PostMapping(value = "/auth-portal-api/register/send-code", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> registerSendCode(@RequestBody EmailInput input, HttpServletRequest request) {
        UUID tenant = interactions.requireTenant(request);
        if (input == null || blank(input.email())) {
            return ResponseEntity.ok(new ApiError(ErrorCodeConstants.REGISTRATION_INPUT_INVALID.code(), ErrorCodeConstants.REGISTRATION_INPUT_INVALID.message()));
        }
        var style = customization.publicStyle(tenant);
        if (!style.registrationEnabled()) {
            return ResponseEntity.ok(new ApiError(ErrorCodeConstants.REGISTRATION_DISABLED.code(), ErrorCodeConstants.REGISTRATION_DISABLED.message()));
        }
        String email = input.email().strip().toLowerCase();
        if (!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return ResponseEntity.ok(new ApiError(ErrorCodeConstants.REGISTRATION_INPUT_INVALID.code(), ErrorCodeConstants.REGISTRATION_INPUT_INVALID.message()));
        }
        var existing = sql.createQuery(PoolUserEntityTable.$).where(PoolUserEntityTable.$.tenantId().eq(tenant), PoolUserEntityTable.$.email().eq(email)).select(PoolUserEntityTable.$.id()).fetchOneOrNull();
        if (existing != null) {
            return ResponseEntity.ok(new ApiError(ErrorCodeConstants.REGISTRATION_EMAIL_EXISTS.code(), ErrorCodeConstants.REGISTRATION_EMAIL_EXISTS.message()));
        }
        Challenge challenge;
        try {
            challenge = security.issueEmailChallenge("registration", null, tenant, "register", email);
        } catch (DomainException ex) {
            return ResponseEntity.ok(new ApiError(ex.code(), ex.getMessage()));
        }
        mail.send(simpleMessage(email, "Easy1Auth 注册验证码", "您的注册验证码是 " + challenge.code() + "，10分钟内有效。"));
        return ResponseEntity.ok(new ChallengeResult(challenge.token(), challenge.expiresIn()));
    }

    /**
     * 注册第二步：校验验证码并完成注册。
     *
     * <p>验证码校验通过后创建 pool_user（无密码，登录标识为邮箱），username 自动
     * 生成，注册成功后立即建立会话并跳转。</p>
     */
    @PostMapping(value = "/auth-portal-api/register/verify", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> registerVerify(@RequestBody VerifyInput input, HttpServletRequest request, HttpServletResponse response) {
        UUID tenant = interactions.requireTenant(request);
        if (input == null || blank(input.token()) || blank(input.code())) {
            return ResponseEntity.ok(new ApiError(ErrorCodeConstants.REGISTRATION_INPUT_INVALID.code(), ErrorCodeConstants.REGISTRATION_INPUT_INVALID.message()));
        }
        ConsumedEmailChallenge consumed;
        try {
            consumed = security.consumeRegistrationEmailChallenge(input.token(), input.code(), "register");
        } catch (DomainException ex) {
            return ResponseEntity.ok(new ApiError(ErrorCodeConstants.VERIFICATION_CODE_INVALID.code(), ErrorCodeConstants.VERIFICATION_CODE_INVALID.message()));
        }
        String email = consumed.destination();
        if (email == null || sql.createQuery(PoolUserEntityTable.$).where(PoolUserEntityTable.$.tenantId().eq(tenant), PoolUserEntityTable.$.email().eq(email)).select(PoolUserEntityTable.$.id()).fetchOneOrNull() != null) {
            return ResponseEntity.ok(new ApiError(ErrorCodeConstants.REGISTRATION_EMAIL_EXISTS.code(), ErrorCodeConstants.REGISTRATION_EMAIL_EXISTS.message()));
        }
        String username = generateUsername(tenant, email);
        var created = poolUsersService.create(tenant, new PoolUserInput(username, email, null, null, email.substring(0, email.indexOf('@')), null, "active", null, null, Map.of()));
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_POOL_USER"), new SimpleGrantedAuthority("TENANT_" + tenant));
        var auth = UsernamePasswordAuthenticationToken.authenticated(org.springframework.security.core.userdetails.User.withUsername(created.id().toString()).password("").authorities(authorities).build(), null, authorities);
        saveAuthentication(auth, request, response);
        interactions.clearLoginError(request);
        return ResponseEntity.ok(new LoginResult("success", continueUrl(request, response), 0));
    }

    /**
     * 邮箱验证码登录第一步：发送验证码。
     *
     * <p>用户存在且 status=active 时发送验证码；不存在时返回假 token 防止邮箱探测。
     * 无论用户是否存在，响应结构一致。</p>
     */
    @PostMapping(value = "/auth-portal-api/login/email/send-code", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> emailLoginSendCode(@RequestBody EmailInput input, HttpServletRequest request) {
        UUID tenant = interactions.requireTenant(request);
        if (input == null || blank(input.email())) {
            return ResponseEntity.ok(new ApiError(ErrorCodeConstants.LOGIN_INPUT_INVALID.code(), ErrorCodeConstants.LOGIN_INPUT_INVALID.message()));
        }
        String email = input.email().strip().toLowerCase();
        var user = sql.createQuery(PoolUserEntityTable.$).where(PoolUserEntityTable.$.tenantId().eq(tenant), PoolUserEntityTable.$.email().eq(email), PoolUserEntityTable.$.status().eq("active")).select(PoolUserEntityTable.$).fetchOneOrNull();
        if (user == null) {
            return ResponseEntity.ok(new ChallengeResult(security.decoyChallengeToken(), 600));
        }
        Challenge challenge;
        try {
            challenge = security.issueEmailChallenge("pool_user", user.id(), tenant, "email_login", email);
        } catch (DomainException ex) {
            return ResponseEntity.ok(new ApiError(ex.code(), ex.getMessage()));
        }
        mail.send(simpleMessage(email, "Easy1Auth 登录验证码", "您的登录验证码是 " + challenge.code() + "，10分钟内有效。"));
        return ResponseEntity.ok(new ChallengeResult(challenge.token(), challenge.expiresIn()));
    }

    /**
     * 邮箱验证码登录第二步：校验验证码并完成登录。
     *
     * <p>验证码校验通过后建立会话并跳转，与密码登录成功的行为一致。</p>
     */
    @PostMapping(value = "/auth-portal-api/login/email/verify", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> emailLoginVerify(@RequestBody VerifyInput input, HttpServletRequest request, HttpServletResponse response) {
        UUID tenant = interactions.requireTenant(request);
        if (input == null || blank(input.token()) || blank(input.code())) {
            return ResponseEntity.ok(new ApiError(ErrorCodeConstants.LOGIN_INPUT_INVALID.code(), ErrorCodeConstants.LOGIN_INPUT_INVALID.message()));
        }
        ConsumedEmailChallenge consumed;
        try {
            consumed = security.consumeEmailChallenge(input.token(), input.code(), "pool_user", "email_login");
        } catch (DomainException ex) {
            return ResponseEntity.ok(new ApiError(ErrorCodeConstants.VERIFICATION_CODE_INVALID.code(), ErrorCodeConstants.VERIFICATION_CODE_INVALID.message()));
        }
        var user = sql.createQuery(PoolUserEntityTable.$).where(PoolUserEntityTable.$.tenantId().eq(tenant), PoolUserEntityTable.$.id().eq(consumed.subjectId()), PoolUserEntityTable.$.status().eq("active")).select(PoolUserEntityTable.$).fetchOneOrNull();
        if (user == null) {
            return ResponseEntity.ok(new ApiError(ErrorCodeConstants.LOGIN_FAILED.code(), ErrorCodeConstants.LOGIN_FAILED.message()));
        }
        sql.createUpdate(PoolUserEntityTable.$).set(PoolUserEntityTable.$.lastLoginAt(), Instant.now()).set(PoolUserEntityTable.$.updatedAt(), Instant.now()).where(PoolUserEntityTable.$.id().eq(user.id()), PoolUserEntityTable.$.tenantId().eq(tenant)).execute();
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_POOL_USER"), new SimpleGrantedAuthority("TENANT_" + tenant));
            var auth = UsernamePasswordAuthenticationToken.authenticated(org.springframework.security.core.userdetails.User.withUsername(user.id().toString()).password("").authorities(authorities).build(), null, authorities);
            bindPendingSocialIdentity(request, user.id());
            saveAuthentication(auth, request, response);
        interactions.clearLoginError(request);
        return ResponseEntity.ok(new LoginResult("success", continueUrl(request, response), 0));
    }

    /**
     * 根据邮箱前缀生成唯一用户名。
     *
     * <p>取邮箱 @ 前缀作为基础用户名；若已被占用则追加随机后缀，确保满足
     * {@code (tenant_id, username)} 唯一约束。</p>
     */
    private String generateUsername(UUID tenant, String email) {
        String base = email.substring(0, email.indexOf('@'));
        if (base.isBlank()) {
            base = "user";
        }
        base = base.replaceAll("[^a-zA-Z0-9._-]", "").strip();
        if (base.isBlank()) {
            base = "user";
        }
        String candidate = base;
        int attempts = 0;
        var table = PoolUserEntityTable.$;
        while (attempts < 10) {
            var conflict = sql.createQuery(table).where(table.tenantId().eq(tenant), table.username().eq(candidate)).select(table.id()).fetchOneOrNull();
            if (conflict == null) {
                return candidate;
            }
            candidate = base + "_" + UUID.randomUUID().toString().substring(0, 8);
            attempts++;
        }
        return base + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /** 构造一封简单文本邮件。 */
    private org.springframework.mail.SimpleMailMessage simpleMessage(String to, String subject, String text) {
        var msg = new org.springframework.mail.SimpleMailMessage();
        msg.setFrom(mailFrom);
        msg.setTo(to);
        msg.setSubject(subject);
        msg.setText(text);
        return msg;
    }

    /**
     * 校验 MFA 动态验证码并完成登录。
     *
     * <p>挑战、用户和租户必须同时存在于同一个 Session 中，且验证码只能被消费一次；
     * 校验成功后清理挑战相关 Session 属性，防止重复提交。</p>
     */
    @PostMapping(value = "/auth-portal-api/mfa", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> verifyMfa(@RequestBody MfaInput input, HttpServletRequest request, HttpServletResponse response) {
        if (input == null || blank(input.code())) {
            return ResponseEntity.ok(new ApiError(ErrorCodeConstants.MFA_INPUT_INVALID.code(), ErrorCodeConstants.MFA_INPUT_INVALID.message()));
        }
        var session = request.getSession(false);
        if (session == null) {
            return ResponseEntity.ok(new ApiError(ErrorCodeConstants.MFA_EXPIRED.code(), ErrorCodeConstants.MFA_EXPIRED.message()));
        }
        String challenge = (String) session.getAttribute("EASY1AUTH_MFA_CHALLENGE");
        UUID expected = uuid(session.getAttribute("EASY1AUTH_MFA_USER"));
        UUID tenant = uuid(session.getAttribute("EASY1AUTH_MFA_TENANT"));
        if (challenge == null || expected == null || tenant == null) {
            return ResponseEntity.ok(new ApiError(ErrorCodeConstants.MFA_EXPIRED.code(), ErrorCodeConstants.MFA_EXPIRED.message()));
        }
        try {
            UUID actual = security.consumeTotpChallenge(challenge, input.code(), "pool_user", "oidc_login");
            if (!actual.equals(expected)) {
                return ResponseEntity.ok(new ApiError(ErrorCodeConstants.MFA_FAILED.code(), ErrorCodeConstants.MFA_FAILED.message()));
            }
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_POOL_USER"), new SimpleGrantedAuthority("TENANT_" + tenant), new SimpleGrantedAuthority("MFA_VERIFIED"));
            var auth = UsernamePasswordAuthenticationToken.authenticated(org.springframework.security.core.userdetails.User.withUsername(actual.toString()).password("").authorities(authorities).build(), null, authorities);
            bindPendingSocialIdentity(request, actual);
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
        if (input == null) {
            return ResponseEntity.ok(new ApiError(ErrorCodeConstants.AUTH_INPUT_INVALID.code(), ErrorCodeConstants.AUTH_INPUT_INVALID.message()));
        }
        var continuation = interactions.consumeConsent(request, input.action());
        return ResponseEntity.ok(continuation);
    }

    /** 发起指定租户和身份源的社会化登录，并将回调地址交给身份源。 */
    @GetMapping("/t/{tenant}/social/{sourceId}/authorize")
    void socialStart(@PathVariable UUID tenant, @PathVariable UUID sourceId, HttpServletRequest request, HttpServletResponse response) throws IOException {
        UUID interactionTenant = interactions.requireTenant(request);
        if (!tenant.equals(interactionTenant)) {
            throw new DomainException(ErrorCodeConstants.AUTH_INTERACTION_MISMATCH_CLIENT_TENANT);
        }
        response.sendRedirect(socialIdentity.authorize(tenant, sourceId, socialCallbackUrl(request)).authorizeUrl());
    }

    /**
     * 前端回调页提交飞书授权码。服务端校验 state 后，已绑定用户直接登录；未绑定用户
     * 则将已验证的外部身份暂存于当前 Session，等待用户创建新账号或认证已有账号。
     *
     * <p>飞书重定向先进入前端页面，页面可将失败原因以 toast 展示，避免供应商错误页直出。</p>
     */
    @PostMapping(value = "/auth-portal-api/social/callback", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> socialCallback(@RequestBody SocialCallbackInput input, HttpServletRequest request, HttpServletResponse response) {
        UUID tenant = interactions.requireTenant(request);
        if (input == null || blank(input.code()) || blank(input.state())) {
            return ResponseEntity.ok(new ApiError(ErrorCodeConstants.AUTH_INPUT_INVALID.code(), ErrorCodeConstants.AUTH_INPUT_INVALID.message()));
        }
        var result = socialIdentity.callback(input.code(), input.state(), socialCallbackUrl(request));
        if (!tenant.equals(result.tenantId())) {
            throw new DomainException(ErrorCodeConstants.AUTH_INTERACTION_MISMATCH_CLIENT_TENANT);
        }
        if (result.poolUserId() != null) {
            saveAuthentication(poolUserAuthentication(result.poolUserId(), tenant, false), request, response);
            interactions.clearLoginError(request);
            return ResponseEntity.ok(SocialCallbackResult.success(continueUrl(request, response)));
        }
        request.getSession(true).setAttribute(SOCIAL_PENDING_IDENTITY, result.identity());
        var identity = result.identity();
        return ResponseEntity.ok(SocialCallbackResult.unbound(identity.name(), identity.email(), identity.username(),
                identity.email() != null && !identity.email().isBlank()));
    }

    /** 用户确认后创建新 pool_user，并绑定当前 Session 中已验证的外部身份。 */
    @PostMapping(value = "/auth-portal-api/social/provision", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<?> socialProvision(@RequestBody SocialProvisionInput input, HttpServletRequest request, HttpServletResponse response) {
        UUID tenant = interactions.requireTenant(request);
        var identity = pendingSocialIdentity(request, tenant);
        if (input == null || blank(input.username())) {
            return ResponseEntity.ok(new ApiError(ErrorCodeConstants.AUTH_INPUT_INVALID.code(), ErrorCodeConstants.AUTH_INPUT_INVALID.message()));
        }
        UUID userId = socialIdentity.provision(identity, input.username().strip());
        clearPendingSocialIdentity(request);
        saveAuthentication(poolUserAuthentication(userId, tenant, false), request, response);
        interactions.clearLoginError(request);
        return ResponseEntity.ok(new LoginResult("success", continueUrl(request, response), 0));
    }

    /** 将认证结果写入当前请求和 HTTP Session，供后续请求复用。 */
    private void saveAuthentication(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContexts.saveContext(context, request, response);
    }

    /** 若 Session 中暂存了已验证的外部身份，则将其绑定到本次登录的本地用户上。 */
    private void bindPendingSocialIdentity(HttpServletRequest request, UUID poolUserId) {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute(SOCIAL_PENDING_IDENTITY) instanceof PendingIdentity identity)) {
            return;
        }
        UUID tenant = interactions.requireTenant(request);
        if (!tenant.equals(identity.tenantId())) {
            throw new DomainException(ErrorCodeConstants.AUTH_INTERACTION_MISMATCH_CLIENT_TENANT);
        }
        socialIdentity.bind(identity, poolUserId);
        session.removeAttribute(SOCIAL_PENDING_IDENTITY);
    }

    /** 读取当前 Session 中属于指定租户的已验证外部身份；缺失或租户不符时抛过期异常。 */
    private PendingIdentity pendingSocialIdentity(HttpServletRequest request, UUID tenant) {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute(SOCIAL_PENDING_IDENTITY) instanceof PendingIdentity identity)
                || !tenant.equals(identity.tenantId())) {
            throw new DomainException(ErrorCodeConstants.AUTH_INTERACTION_EXPIRED_LOGIN);
        }
        return identity;
    }

    /** 清理 Session 中暂存的外部身份。 */
    private void clearPendingSocialIdentity(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute(SOCIAL_PENDING_IDENTITY);
        }
    }

    /** 构造 pool_user 的已认证主体，可按需附加 MFA_VERIFIED 权限。 */
    private static Authentication poolUserAuthentication(UUID userId, UUID tenant, boolean mfaVerified) {
        var authorities = new ArrayList<SimpleGrantedAuthority>(List.of(
                new SimpleGrantedAuthority("ROLE_POOL_USER"), new SimpleGrantedAuthority("TENANT_" + tenant)));
        if (mfaVerified) {
            authorities.add(new SimpleGrantedAuthority("MFA_VERIFIED"));
        }
        return UsernamePasswordAuthenticationToken.authenticated(org.springframework.security.core.userdetails.User.withUsername(userId.toString()).password("").authorities(authorities).build(), null, authorities);
    }

    /** 基于当前请求 URL 构造前端回调页地址，供身份源回跳使用。 */
    private static String socialCallbackUrl(HttpServletRequest request) {
        URI requestUri = URI.create(request.getRequestURL().toString());
        return requestUri.resolve(request.getContextPath() + "/oauth-login/social/callback").toString();
    }

    /**
     * 取得登录前被 Spring Security 保存的请求地址。
     *
     * <p>读取后立即移除，避免同一个 saved request 被重复消费；不存在时回到门户根路径。</p>
     */
    private String continueUrl(HttpServletRequest request, HttpServletResponse response) {
        SavedRequest saved = requestCache.getRequest(request, response);
        if (saved == null) {
            return "/";
        }
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

    /**
     * 密码登录请求体。
     *
     * @param username 登录邮箱（登录标识）
     * @param password 登录密码
     */
    public record LoginInput(String username, String password) { }
    /**
     * 邮箱输入请求体，用于发送验证码。
     *
     * @param email 目标邮箱地址
     */
    public record EmailInput(String email) { }
    /**
     * 验证码校验请求体。
     *
     * @param token 发送验证码时返回的不透明挑战 token
     * @param code  用户收到的验证码
     */
    public record VerifyInput(String token, String code) { }
    /**
     * MFA 验证请求体。
     *
     * @param code 用户输入的 TOTP 动态验证码
     */
    public record MfaInput(String code) { }
    /**
     * 授权确认操作请求体。
     *
     * @param action 用户决定：approve（同意）或 deny（拒绝）
     */
    public record ConsentInput(String action) { }
    /**
     * 飞书前端回调提交的授权码与防 CSRF state。
     *
     * @param code  身份源回调返回的授权码
     * @param state 发起授权时携带的防 CSRF state
     */
    public record SocialCallbackInput(String code, String state) { }
    /**
     * 用户确认创建新账号时提供的本地用户名。
     *
     * @param username 本地用户名
     */
    public record SocialProvisionInput(String username) { }
    /**
     * 社交登录回调结果，前端根据 status 显示继续登录或账户确认界面。
     *
     * @param status            状态：success（可直接登录）或 unbound（需绑定/新建账号）
     * @param redirectUrl       成功后继续跳转的地址
     * @param name              外部身份显示名称
     * @param email             外部身份邮箱
     * @param suggestedUsername 建议的本地用户名
     * @param canProvision      是否允许用该外部身份创建新账号
     */
    public record SocialCallbackResult(String status, String redirectUrl, String name, String email, String suggestedUsername, boolean canProvision) {
        static SocialCallbackResult success(String redirectUrl) { return new SocialCallbackResult("success", redirectUrl, null, null, null, false); }
        static SocialCallbackResult unbound(String name, String email, String suggestedUsername, boolean canProvision) { return new SocialCallbackResult("unbound", null, name, email, suggestedUsername, canProvision); }
    }
    /**
     * 登录结果；MFA 阶段返回剩余有效秒数，成功时返回继续地址。
     *
     * @param status      状态：success / mfa_required
     * @param redirectUrl 成功后继续跳转的地址（MFA 阶段为 null）
     * @param expiresIn   挑战剩余有效秒数（非 MFA 场景为 0）
     */
    public record LoginResult(String status, String redirectUrl, int expiresIn) { }
    /**
     * 验证码挑战结果，返回不透明 token 和有效秒数。
     *
     * @param token     不透明挑战 token
     * @param expiresIn 挑战有效秒数
     */
    public record ChallengeResult(String token, int expiresIn) { }
    /**
     * 门户接口统一错误响应。
     *
     * @param code    业务错误码
     * @param message 错误提示信息
     */
    public record ApiError(int code, String message) { }
}
