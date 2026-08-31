package com.easy1auth.admin.web;

import com.easy1auth.admin.constant.ErrorCodeConstants;
import com.easy1auth.admin.mq.producer.MailSendProducer;
import com.easy1auth.admin.web.dto.*;
import com.easy1auth.admin.config.AdminJwtProperties;
import com.easy1auth.admin.security.AdminTokenService;
import com.easy1auth.admin.annotation.ManagementRouteClassification;
import com.easy1auth.admin.constant.ManagementRouteKind;
import com.easy1auth.adminidentity.dto.AdminAccount;
import com.easy1auth.adminidentity.service.AdminIdentityService;
import com.easy1auth.adminidentity.service.RegistrationCodeService;
import com.easy1auth.common.foundation.error.DomainException;
import com.easy1auth.common.foundation.web.ApiResponse;
import com.easy1auth.common.foundation.util.WebFrameworkUtils;
import com.easy1auth.tenant.service.TenantService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import com.easy1auth.admin.config.RegistrationProperties;
import com.easy1auth.admin.mq.message.MailSendMessage;
import com.easy1auth.security.service.SecurityPolicyService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.*;

/**
 * 管理端认证接口。
 *
 * <p>管理端 REST 入口，基路径 {@code /api/auth}，提供管理账号（admin_user）的
 * 密码 / 邮箱验证码登录、MFA 二次验证、注册、验证码发送、令牌刷新与退出登录等
 * 认证能力。认证类路由均为公开或登录前路由，以 {@link ApiResponse} 统一包装返回。</p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    /** 管理账号身份服务（认证、注册、令牌轮换） */
    private final AdminIdentityService identities;
    /** 管理端访问令牌签发服务 */
    private final AdminTokenService tokens;
    /** 租户服务（登录后解析账号的租户列表） */
    private final TenantService tenants;
    /** 管理端 JWT 配置 */
    private final AdminJwtProperties jwt;
    /** 公共注册服务 */
    private final PublicRegistrationService registrations;
    /** 注册验证码服务 */
    private final RegistrationCodeService registrationCodes;
    /** 注册功能配置 */
    private final RegistrationProperties registration;
    /** 安全策略服务（挑战码、MFA、密码校验） */
    private final SecurityPolicyService security;
    /** Redis Stream 消息生产者（发送验证码邮件） */
    private final MailSendProducer mailSendProducer;

    AuthController(AdminIdentityService identities, AdminTokenService tokens, TenantService tenants, AdminJwtProperties jwt, PublicRegistrationService registrations, RegistrationCodeService registrationCodes, RegistrationProperties registration, SecurityPolicyService security, MailSendProducer mailSendProducer) {
        this.identities = identities;
        this.tokens = tokens;
        this.tenants = tenants;
        this.jwt = jwt;
        this.registrations = registrations;
        this.registrationCodes = registrationCodes;
        this.registration = registration;
        this.security = security;
        this.mailSendProducer = mailSendProducer;
    }

    /** 管理账号登录：支持密码登录与邮箱验证码登录；已开启 MFA 时返回 MFA 挑战。 */
    @ManagementRouteClassification(ManagementRouteKind.AUTHENTICATION)
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request, HttpServletRequest http) {
        if ("email".equals(request.loginType())) {
            return emailLogin(request, http);
        }
        if (!"password".equals(request.loginType())) {
            throw new DomainException(ErrorCodeConstants.LOGIN_TYPE_UNSUPPORTED);
        }
        var result = identities.authenticate(request.username(), request.password(), jwt.refreshTtl(), WebFrameworkUtils.getUserAgent(http), http.getRemoteAddr());
        if (result.account().mfaEnabled()) {
            identities.logout(result.refreshToken());
            var challenge = security.issueTotpChallenge("admin", result.account().id(), null, "login");
            return ApiResponse.ok(LoginResponse.mfa(challenge.token(), challenge.expiresIn()));
        }
        return ApiResponse.ok(response(result.account(), result.refreshToken()));
    }

    /** 校验登录 MFA 挑战并完成登录，返回访问与刷新令牌。 */
    @ManagementRouteClassification(ManagementRouteKind.AUTHENTICATION)
    @PostMapping("/mfa/verify")
    public ApiResponse<LoginResponse> verifyMfa(@RequestBody MfaLoginRequest request, HttpServletRequest http) {
        UUID account = security.consumeTotpChallenge(request.challengeToken(), request.code(), "admin", "login");
        var result = identities.completeMfa(account, jwt.refreshTtl(), WebFrameworkUtils.getUserAgent(http), http.getRemoteAddr());
        return ApiResponse.ok(response(result.account(), result.refreshToken()));
    }

    /** 管理账号注册：创建账号并自动创建默认租户，注册成功后直接登录。 */
    @ManagementRouteClassification(ManagementRouteKind.AUTHENTICATION)
    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@RequestBody RegisterRequest request, HttpServletRequest http) {
        String username = request.username() == null || request.username().isBlank() ? request.email().split("@", 2)[0] : request.username();
        var result = registrations.register(username, request.email(), request.password(), request.code(), WebFrameworkUtils.getUserAgent(http), http.getRemoteAddr());
        return ApiResponse.ok(response(result.identity().account(), result.identity().refreshToken()), "注册成功");
    }

    /** 发送验证码：支持注册验证码与邮箱登录验证码（未注册邮箱返回混淆令牌防探测）。 */
    @ManagementRouteClassification(ManagementRouteKind.AUTHENTICATION)
    @PostMapping("/send-code")
    @Transactional
    public ApiResponse<SendCodeResponse> sendCode(@RequestBody SendCodeRequest request) {
        String code = null;
        String challengeToken = null;
        if ("register".equals(request.type())) {
            var issued = registrationCodes.issue(request.email());
            sendEmail(issued.email(), "Easy1Auth 注册验证码", "您的验证码是 " + issued.code() + "，10分钟内有效。");
            if (registration.exposeCode()) {
                code = issued.code();
            }
        } else if ("login".equals(request.type())) {
            challengeToken = security.decoyChallengeToken();
            var account = identities.activeAccountByEmail(request.email());
            if (account.isPresent()) {
                var challenge = security.issueEmailChallenge("admin", account.get().id(), null, "login", account.get().email());
                challengeToken = challenge.token();
                sendEmail(account.get().email(), "Easy1Auth 登录验证码", "您的登录验证码是 " + challenge.code() + "，10分钟内有效。");
                if (registration.exposeCode()) {
                    code = challenge.code();
                }
            }
        } else {
            throw new DomainException(ErrorCodeConstants.CODE_TYPE_UNSUPPORTED);
        }
        return ApiResponse.ok(new SendCodeResponse(code, challengeToken), "验证码已进入发送队列");
    }

    /** 用刷新令牌换取新的访问令牌与替换后的刷新令牌。 */
    @ManagementRouteClassification(ManagementRouteKind.AUTHENTICATION)
    @PostMapping("/refresh")
    public ApiResponse<RefreshResponse> refresh(@RequestBody RefreshRequest request, HttpServletRequest http) {
        var result = identities.rotate(request.refreshToken(), jwt.refreshTtl(), WebFrameworkUtils.getUserAgent(http), http.getRemoteAddr());
        return ApiResponse.ok(new RefreshResponse(tokens.issue(result.account()), result.replacementToken()));
    }

    /** 退出登录：携带刷新令牌时注销该令牌，否则注销当前账号的全部会话。 */
    @ManagementRouteClassification(ManagementRouteKind.AUTHENTICATION)
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody(required = false) RefreshRequest request, @AuthenticationPrincipal Jwt principal) {
        if (request != null && request.refreshToken() != null) {
            identities.logout(request.refreshToken());
        } else {
            identities.logoutAllAndInvalidate(UUID.fromString(principal.getSubject()));
        }
        return ApiResponse.ok(null, "退出成功");
    }

    /** 邮箱验证码登录内部实现：校验挑战后完成登录，已开启 MFA 时转 MFA 挑战。 */
    private ApiResponse<LoginResponse> emailLogin(LoginRequest request, HttpServletRequest http) {
        var challenge = security.consumeEmailChallenge(request.challengeToken(), request.code(), "admin", "login");
        var account = identities.account(challenge.subjectId());
        if (challenge.destination() == null || !account.email().equalsIgnoreCase(challenge.destination())) {
            throw new DomainException(ErrorCodeConstants.MFA_CHALLENGE_INVALID);
        }
        if (account.mfaEnabled()) {
            var totp = security.issueTotpChallenge("admin", account.id(), null, "login");
            return ApiResponse.ok(LoginResponse.mfa(totp.token(), totp.expiresIn()));
        }
        var result = identities.completeMfa(account.id(), jwt.refreshTtl(), WebFrameworkUtils.getUserAgent(http), http.getRemoteAddr());
        return ApiResponse.ok(response(result.account(), result.refreshToken()));
    }

    /** 组装登录成功响应：签发访问令牌并附带账号可访问的租户列表与当前租户。 */
    private LoginResponse response(AdminAccount account, String refresh) {
        var memberships = tenants.list(account.id());
        UUID current = account.lastTenantId();
        boolean currentAccessible = current != null && memberships.stream().map(t -> t.id()).anyMatch(current::equals);
        if (!currentAccessible) {
            current = memberships.isEmpty() ? null : memberships.getFirst().id();
        }
        return LoginResponse.success(tokens.issue(account), refresh, new LoginUser(account.id(), account.username(), account.email(), null, current), memberships);
    }

    /** 发布验证码邮件消息，由 Redis Stream 消费者异步发送。 */
    private void sendEmail(String recipient, String subject, String body) {
        mailSendProducer.sendMailMessage(new MailSendMessage(recipient,subject,body));
    }

    /**
     * 登录请求。
     *
     * @param username       用户名（密码登录时使用）
     * @param password       密码（密码登录时使用）
     * @param email          邮箱（邮箱验证码登录时使用）
     * @param code           验证码（邮箱验证码登录时使用）
     * @param challengeToken 邮箱登录挑战凭证（可选）
     * @param loginType      登录方式：password / email
     */

    /**
     * 注册请求。
     *
     * @param email    邮箱（必填，用作账号标识）
     * @param password 密码
     * @param code     注册验证码
     * @param username 用户名（可选，缺省时取邮箱前缀）
     */

    /**
     * 刷新令牌请求。
     *
     * @param refreshToken 刷新令牌
     */

    /**
     * 发送验证码请求。
     *
     * @param email 目标邮箱
     * @param type  验证码用途：register（注册）/ login（登录）
     */

    /**
     * 发送验证码响应（仅在配置允许时返回明文验证码）。
     *
     * @param code           明文验证码（可为 null）
     * @param challengeToken 挑战令牌（登录场景返回）
     */

    /**
     * 刷新响应。
     *
     * @param token        新签发的访问令牌
     * @param refreshToken 替换后的刷新令牌
     */

    /**
     * MFA 登录请求。
     *
     * @param challengeToken MFA 挑战凭证
     * @param code           TOTP 验证码
     */

    /**
     * 登录用户信息。
     *
     * @param id              账号 ID
     * @param username        用户名
     * @param email           邮箱
     * @param avatar          头像地址（可为 null）
     * @param currentTenantId 当前选中的租户 ID（可为 null）
     */

    /**
     * 登录响应。
     *
     * @param status         状态：success / mfa_required
     * @param token          访问令牌（登录成功时返回）
     * @param refreshToken   刷新令牌（登录成功时返回）
     * @param user           登录用户信息（登录成功时返回）
     * @param tenants        账号可访问的租户列表
     * @param challengeToken MFA 挑战凭证（需要 MFA 时返回）
     * @param methods        可用的 MFA 方式（需要 MFA 时返回）
     * @param expiresIn      MFA 挑战有效秒数（需要 MFA 时返回）
     */
}
