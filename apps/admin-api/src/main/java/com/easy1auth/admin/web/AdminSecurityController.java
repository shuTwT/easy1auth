package com.easy1auth.admin.web;

import com.easy1auth.adminidentity.*;
import com.easy1auth.admin.security.ManagementRouteClassification;
import com.easy1auth.admin.security.ManagementRouteKind;
import com.easy1auth.audit.DeliveryService;
import com.easy1auth.foundation.web.ApiResponse;
import com.easy1auth.security.SecurityPolicyService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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
        return ApiResponse.ok(Map.of("policy", SecurityPolicyService.adminPolicy(), "expiryStatus", Map.of("expired", false, "daysUntilExpiry", 90)));
    }

    @ManagementRouteClassification(ManagementRouteKind.AUTHENTICATED_SELF)
    @PostMapping("/change-password")
    public ApiResponse<Void> change(@AuthenticationPrincipal Jwt jwt, @RequestBody ChangePassword input) {
        if (!Objects.equals(input.newPassword(), input.confirmPassword()))
            throw new com.easy1auth.foundation.error.DomainException("PASSWORD_CONFIRM_MISMATCH", "两次输入的密码不一致", 400);
        security.validatePassword(input.newPassword(), SecurityPolicyService.adminPolicy());
        identities.changePassword(id(jwt), input.currentPassword(), input.newPassword());
        return ApiResponse.ok(null, "密码修改成功，请重新登录");
    }

    @ManagementRouteClassification(ManagementRouteKind.AUTHENTICATED_SELF)
    @GetMapping("/mfa/status")
    public ApiResponse<?> status(@AuthenticationPrincipal Jwt jwt) {
        var status = security.status("admin", id(jwt));
        return ApiResponse.ok(Map.of("enabled", status.enabled(), "type", status.methods().isEmpty() ? "" : status.methods().getFirst()));
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
        return ApiResponse.ok(Map.of("challengeToken", c.token()), "验证码已发送");
    }

    private static UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    private static ApiResponse<Void> ok(String message) {
        return ApiResponse.ok(null, message);
    }

    public record ChangePassword(String currentPassword, String newPassword, String confirmPassword) {
    }

    public record TokenInput(String token, String challengeToken, String type) {
    }
}
