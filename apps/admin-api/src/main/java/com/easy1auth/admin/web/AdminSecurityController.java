package com.easy1auth.admin.web;

import com.easy1auth.admin.constant.ErrorCodeConstants;
import com.easy1auth.admin.mq.producer.MailSendProducer;
import com.easy1auth.admin.web.dto.*;
import com.easy1auth.admin.annotation.ManagementRouteClassification;
import com.easy1auth.admin.constant.ManagementRouteKind;
import com.easy1auth.adminidentity.service.AdminIdentityService;
import com.easy1auth.admin.mq.message.MailSendMessage;
import com.easy1auth.common.foundation.web.ApiResponse;
import com.easy1auth.common.foundation.error.DomainException;
import com.easy1auth.security.service.SecurityPolicyService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 管理账号自身安全设置接口。
 *
 * <p>管理端 REST 入口，基路径 {@code /api/security}，面向已登录的管理账号提供
 * 个人资料、密码策略、密码修改、邮箱换绑与 MFA 二次验证等自助安全能力。
 * 所有接口均要求账号已登录（{@code AUTHENTICATED_SELF}），并以 {@link ApiResponse}
 * 统一包装返回。</p>
 */
@RestController
@RequestMapping("/api/security")
public class AdminSecurityController {
    /** 管理账号身份服务 */
    private final AdminIdentityService identities;
    /** 安全策略服务（密码校验、挑战码、MFA） */
    private final SecurityPolicyService security;
    /** Redis Stream 消息生产者（发送验证码邮件） */
    private final MailSendProducer mailSendProducer;

    AdminSecurityController(AdminIdentityService identities, SecurityPolicyService security, MailSendProducer mailSendProducer) {
        this.identities = identities;
        this.security = security;
        this.mailSendProducer = mailSendProducer;
    }

    /** 查询管理账号适用的密码策略及当前密码的过期状态。 */
    @ManagementRouteClassification(ManagementRouteKind.AUTHENTICATED_SELF)
    @GetMapping("/password-policy")
    public ApiResponse<?> policy(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(new PasswordPolicyResponse(SecurityPolicyService.adminPolicy(), new ExpiryStatus(false, 90)));
    }

    /** 查询当前管理账号的个人资料。 */
    @ManagementRouteClassification(ManagementRouteKind.AUTHENTICATED_SELF)
    @GetMapping("/profile")
    public ApiResponse<?> profile(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(identities.account(id(jwt)));
    }

    /** 更新当前管理账号的个人资料（用户名、手机号）。 */
    @ManagementRouteClassification(ManagementRouteKind.AUTHENTICATED_SELF)
    @PutMapping("/profile")
    public ApiResponse<?> updateProfile(@AuthenticationPrincipal Jwt jwt, @RequestBody AdminSecurityProfileInput input) {
        return ApiResponse.ok(identities.updateOwnProfile(id(jwt), input.username(), input.phone()), "个人资料更新成功");
    }

    /** 发起邮箱换绑：向新邮箱发送验证码，返回挑战凭证。 */
    @Transactional
    @ManagementRouteClassification(ManagementRouteKind.AUTHENTICATED_SELF)
    @PostMapping("/email/send-code")
    public ApiResponse<?> sendEmailChangeCode(@AuthenticationPrincipal Jwt jwt, @RequestBody EmailChangeRequest input) {
        UUID accountId = id(jwt);
        String email = identities.prepareOwnEmailChange(accountId, input.email());
        var challenge = security.issueEmailChallenge("admin", accountId, null, "email_change", email);
        sendEmail(email, "Easy1Auth 邮箱换绑验证码", "您的邮箱换绑验证码是 " + challenge.code() + "，10分钟内有效。");
        return ApiResponse.ok(new ChallengeResponse(challenge.token(), challenge.expiresIn()), "验证码已发送到新邮箱");
    }

    /** 校验验证码并完成邮箱换绑，成功后需重新登录。 */
    @Transactional
    @ManagementRouteClassification(ManagementRouteKind.AUTHENTICATED_SELF)
    @PostMapping("/email/verify")
    public ApiResponse<?> verifyEmailChange(@AuthenticationPrincipal Jwt jwt, @RequestBody EmailVerifyRequest input) {
        UUID accountId = id(jwt);
        var challenge = security.consumeEmailChallenge(input.challengeToken(), input.code(), "admin", "email_change");
        if (!accountId.equals(challenge.subjectId()) || challenge.destination() == null) {
            throw new DomainException(ErrorCodeConstants.EMAIL_CHANGE_CHALLENGE_INVALID);
        }
        var account = identities.changeOwnEmail(accountId, challenge.destination());
        return ApiResponse.ok(new EmailChangeResponse(account.email()), "邮箱换绑成功，请重新登录");
    }

    /** 修改当前管理账号的登录密码（需校验原密码），成功后需重新登录。 */
    @ManagementRouteClassification(ManagementRouteKind.AUTHENTICATED_SELF)
    @PostMapping("/change-password")
    public ApiResponse<Void> change(@AuthenticationPrincipal Jwt jwt, @RequestBody ChangePassword input) {
        if (!Objects.equals(input.newPassword(), input.confirmPassword())) {
            throw new DomainException(ErrorCodeConstants.PASSWORD_CONFIRM_MISMATCH);
        }
        security.validatePassword(input.newPassword(), SecurityPolicyService.adminPolicy());
        identities.changePassword(id(jwt), input.currentPassword(), input.newPassword());
        return ApiResponse.ok(null, "密码修改成功，请重新登录");
    }

    /** 查询当前管理账号的 MFA 启用状态与方式。 */
    @ManagementRouteClassification(ManagementRouteKind.AUTHENTICATED_SELF)
    @GetMapping("/mfa/status")
    public ApiResponse<?> status(@AuthenticationPrincipal Jwt jwt) {
        var status = security.status("admin", id(jwt));
        return ApiResponse.ok(new MfaStatusResponse(status.enabled(), status.methods().isEmpty() ? "" : status.methods().getFirst()));
    }

    /** 发起 MFA 绑定：生成 TOTP 密钥与二维码信息。 */
    @ManagementRouteClassification(ManagementRouteKind.AUTHENTICATED_SELF)
    @PostMapping("/mfa/setup")
    public ApiResponse<?> setup(@AuthenticationPrincipal Jwt jwt) {
        var account = identities.account(id(jwt));
        return ApiResponse.ok(security.setupTotp("admin", account.id(), null, account.email()));
    }

    /** 提交 TOTP 验证码以启用 MFA，成功后需重新登录。 */
    @ManagementRouteClassification(ManagementRouteKind.AUTHENTICATED_SELF)
    @PostMapping("/mfa/enable")
    public ApiResponse<Void> enable(@AuthenticationPrincipal Jwt jwt, @RequestBody TokenInput in) {
        security.enableTotp("admin", id(jwt), in.token());
        identities.enableMfa(id(jwt), "totp");
        return ok("MFA 已启用，请重新登录");
    }

    /** 提交 TOTP 验证码以关闭 MFA，成功后需重新登录。 */
    @ManagementRouteClassification(ManagementRouteKind.AUTHENTICATED_SELF)
    @PostMapping("/mfa/disable")
    public ApiResponse<Void> disable(@AuthenticationPrincipal Jwt jwt, @RequestBody TokenInput in) {
        security.disable("admin", id(jwt), in.token());
        identities.disableMfa(id(jwt));
        return ok("MFA 已禁用，请重新登录");
    }

    /** 校验 TOTP 验证码（用于登录后的二次验证）。 */
    @ManagementRouteClassification(ManagementRouteKind.AUTHENTICATED_SELF)
    @PostMapping("/mfa/verify")
    public ApiResponse<Void> verify(@AuthenticationPrincipal Jwt jwt, @RequestBody TokenInput in) {
        security.verifyTotp("admin", id(jwt), in.token());
        return ok("MFA 验证成功");
    }

    /** 向当前账号邮箱发送邮件验证码（用于敏感操作升级验证）。 */
    @ManagementRouteClassification(ManagementRouteKind.AUTHENTICATED_SELF)
    @PostMapping("/mfa/send-email-code")
    public ApiResponse<?> email(@AuthenticationPrincipal Jwt jwt) {
        var account = identities.account(id(jwt));
        var c = security.issueEmailChallenge("admin", account.id(), null, "step_up");
        sendEmail(account.email(), "Easy1Auth 安全验证码", "您的验证码是 " + c.code() + "，10分钟内有效。");
        return ApiResponse.ok(new ChallengeTokenResponse(c.token()), "验证码已发送");
    }

    private static UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    private static ApiResponse<Void> ok(String message) {
        return ApiResponse.ok(null, message);
    }

    /** 发布验证码邮件消息，由 Redis Stream 消费者异步发送。 */
    private void sendEmail(String recipient, String subject, String body) {
        mailSendProducer.sendMailMessage(new MailSendMessage(recipient, subject, body));
    }

    /**
     * 修改密码输入。
     *
     * @param currentPassword 当前密码
     * @param newPassword     新密码
     * @param confirmPassword 确认新密码（需与 newPassword 一致）
     */

    /**
     * 个人资料更新输入。
     *
     * @param username 用户名（可选）
     * @param phone    手机号（可选）
     */

    /**
     * 邮箱换绑请求。
     *
     * @param email 新邮箱地址
     */

    /**
     * 邮箱验证请求。
     *
     * @param challengeToken 换绑挑战凭证
     * @param code           收到的验证码
     */

    /**
     * MFA 令牌输入。
     *
     * @param token           TOTP 验证码或挑战令牌
     * @param challengeToken  挑战凭证（可选）
     * @param type            验证类型（可选）
     */

    /**
     * 密码过期状态。
     *
     * @param expired         是否已过期
     * @param daysUntilExpiry 距离过期剩余天数
     */

    /**
     * 密码策略响应。
     *
     * @param policy        密码策略
     * @param expiryStatus  密码过期状态
     */

    /**
     * 挑战凭证响应。
     *
     * @param challengeToken 挑战令牌
     * @param expiresIn      有效秒数
     */

    /**
     * 邮箱换绑结果。
     *
     * @param email 换绑后的新邮箱
     */

    /**
     * MFA 状态响应。
     *
     * @param enabled 是否已启用 MFA
     * @param type    MFA 方式（如 totp），未启用时为空串
     */

    /**
     * 挑战令牌响应。
     *
     * @param challengeToken 挑战令牌
     */
}
