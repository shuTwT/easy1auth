package com.easy1auth.admin.web;

import com.easy1auth.adminidentity.*;
import com.easy1auth.admin.security.ManagementRouteClassification;
import com.easy1auth.admin.security.ManagementRouteKind;
import com.easy1auth.audit.DeliveryService;
import com.easy1auth.foundation.web.ApiResponse;
import com.easy1auth.security.SecurityPolicyService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/security")
public class AdminSecurityController {
    private final AdminIdentityService identities;
    private final SecurityPolicyService security;
    private final DeliveryService delivery;

    AdminSecurityController(AdminIdentityService identities, SecurityPolicyService security, DeliveryService delivery) {
        this.identities = identities;
        this.security = security;
        this.delivery = delivery;
    }

    @ManagementRouteClassification(ManagementRouteKind.AUTHENTICATED_SELF)
    @GetMapping("/password-policy")
    public ApiResponse<?> policy(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(new PasswordPolicyResponse(SecurityPolicyService.adminPolicy(), new ExpiryStatus(false, 90)));
    }

    @ManagementRouteClassification(ManagementRouteKind.AUTHENTICATED_SELF)
    @GetMapping("/profile")
    public ApiResponse<?> profile(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(identities.account(id(jwt)));
    }

    @ManagementRouteClassification(ManagementRouteKind.AUTHENTICATED_SELF)
    @PutMapping("/profile")
    public ApiResponse<?> updateProfile(@AuthenticationPrincipal Jwt jwt, @RequestBody ProfileInput input) {
        return ApiResponse.ok(identities.updateOwnProfile(id(jwt), input.username(), input.phone()), "个人资料更新成功");
    }

    @Transactional
    @ManagementRouteClassification(ManagementRouteKind.AUTHENTICATED_SELF)
    @PostMapping("/email/send-code")
    public ApiResponse<?> sendEmailChangeCode(@AuthenticationPrincipal Jwt jwt, @RequestBody EmailChangeRequest input) {
        UUID accountId = id(jwt);
        String email = identities.prepareOwnEmailChange(accountId, input.email());
        var challenge = security.issueEmailChallenge("admin", accountId, null, "email_change", email);
        delivery.enqueueEmail(null, email, "Easy1Auth 邮箱换绑验证码", "您的邮箱换绑验证码是 " + challenge.code() + "，10分钟内有效。", "email-change:" + accountId + ":" + java.time.Instant.now().getEpochSecond() / 60);
        return ApiResponse.ok(new ChallengeResponse(challenge.token(), challenge.expiresIn()), "验证码已发送到新邮箱");
    }

    @Transactional
    @ManagementRouteClassification(ManagementRouteKind.AUTHENTICATED_SELF)
    @PostMapping("/email/verify")
    public ApiResponse<?> verifyEmailChange(@AuthenticationPrincipal Jwt jwt, @RequestBody EmailVerifyRequest input) {
        UUID accountId = id(jwt);
        var challenge = security.consumeEmailChallenge(input.challengeToken(), input.code(), "admin", "email_change");
        if (!accountId.equals(challenge.subjectId()) || challenge.destination() == null)
            throw new com.easy1auth.foundation.error.DomainException(ErrorCodeConstants.EMAIL_CHANGE_CHALLENGE_INVALID);
        var account = identities.changeOwnEmail(accountId, challenge.destination());
        return ApiResponse.ok(new EmailChangeResponse(account.email()), "邮箱换绑成功，请重新登录");
    }

    @ManagementRouteClassification(ManagementRouteKind.AUTHENTICATED_SELF)
    @PostMapping("/change-password")
    public ApiResponse<Void> change(@AuthenticationPrincipal Jwt jwt, @RequestBody ChangePassword input) {
        if (!Objects.equals(input.newPassword(), input.confirmPassword()))
            throw new com.easy1auth.foundation.error.DomainException(ErrorCodeConstants.PASSWORD_CONFIRM_MISMATCH);
        security.validatePassword(input.newPassword(), SecurityPolicyService.adminPolicy());
        identities.changePassword(id(jwt), input.currentPassword(), input.newPassword());
        return ApiResponse.ok(null, "密码修改成功，请重新登录");
    }

    @ManagementRouteClassification(ManagementRouteKind.AUTHENTICATED_SELF)
    @GetMapping("/mfa/status")
    public ApiResponse<?> status(@AuthenticationPrincipal Jwt jwt) {
        var status = security.status("admin", id(jwt));
        return ApiResponse.ok(new MfaStatusResponse(status.enabled(), status.methods().isEmpty() ? "" : status.methods().getFirst()));
    }

    @ManagementRouteClassification(ManagementRouteKind.AUTHENTICATED_SELF)
    @PostMapping("/mfa/setup")
    public ApiResponse<?> setup(@AuthenticationPrincipal Jwt jwt) {
        var account = identities.account(id(jwt));
        return ApiResponse.ok(security.setupTotp("admin", account.id(), null, account.email()));
    }

    @ManagementRouteClassification(ManagementRouteKind.AUTHENTICATED_SELF)
    @PostMapping("/mfa/enable")
    public ApiResponse<Void> enable(@AuthenticationPrincipal Jwt jwt, @RequestBody TokenInput in) {
        security.enableTotp("admin", id(jwt), in.token());
        identities.enableMfa(id(jwt), "totp");
        return ok("MFA 已启用，请重新登录");
    }

    @ManagementRouteClassification(ManagementRouteKind.AUTHENTICATED_SELF)
    @PostMapping("/mfa/disable")
    public ApiResponse<Void> disable(@AuthenticationPrincipal Jwt jwt, @RequestBody TokenInput in) {
        security.disable("admin", id(jwt), in.token());
        identities.disableMfa(id(jwt));
        return ok("MFA 已禁用，请重新登录");
    }

    @ManagementRouteClassification(ManagementRouteKind.AUTHENTICATED_SELF)
    @PostMapping("/mfa/verify")
    public ApiResponse<Void> verify(@AuthenticationPrincipal Jwt jwt, @RequestBody TokenInput in) {
        security.verifyTotp("admin", id(jwt), in.token());
        return ok("MFA 验证成功");
    }

    @ManagementRouteClassification(ManagementRouteKind.AUTHENTICATED_SELF)
    @PostMapping("/mfa/send-email-code")
    public ApiResponse<?> email(@AuthenticationPrincipal Jwt jwt) {
        var account = identities.account(id(jwt));
        var c = security.issueEmailChallenge("admin", account.id(), null, "step_up");
        delivery.enqueueEmail(null, account.email(), "Easy1Auth 安全验证码", "您的验证码是 " + c.code() + "，10分钟内有效。", "mfa-email:" + c.token());
        return ApiResponse.ok(new ChallengeTokenResponse(c.token()), "验证码已发送");
    }

    private static UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    private static ApiResponse<Void> ok(String message) {
        return ApiResponse.ok(null, message);
    }

    public record ChangePassword(String currentPassword, String newPassword, String confirmPassword) {
    }

    public record ProfileInput(String username, String phone) {
    }

    public record EmailChangeRequest(String email) {
    }

    public record EmailVerifyRequest(String challengeToken, String code) {
    }

    public record TokenInput(String token, String challengeToken, String type) {
    }

    public record ExpiryStatus(boolean expired, int daysUntilExpiry) {
    }

    public record PasswordPolicyResponse(SecurityPolicyService.Policy policy, ExpiryStatus expiryStatus) {
    }

    public record ChallengeResponse(String challengeToken, int expiresIn) {
    }

    public record EmailChangeResponse(String email) {
    }

    public record MfaStatusResponse(boolean enabled, String type) {
    }

    public record ChallengeTokenResponse(String challengeToken) {
    }
}
