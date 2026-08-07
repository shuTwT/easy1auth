package com.easy1auth.admin.web;

import com.easy1auth.admin.config.AdminJwtProperties;
import com.easy1auth.admin.security.AdminTokenService;
import com.easy1auth.admin.security.ManagementRouteClassification;
import com.easy1auth.admin.security.ManagementRouteKind;
import com.easy1auth.adminidentity.*;
import com.easy1auth.foundation.error.DomainException;
import com.easy1auth.foundation.web.ApiResponse;
import com.easy1auth.tenant.WebFramework;
import com.easy1auth.tenant.TenantService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import com.easy1auth.admin.config.RegistrationProperties;
import com.easy1auth.security.SecurityPolicyService;
import com.easy1auth.audit.DeliveryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AdminIdentityService identities;
    private final AdminTokenService tokens;
    private final TenantService tenants;
    private final AdminJwtProperties jwt;
    private final PublicRegistrationService registrations;
    private final RegistrationCodeService registrationCodes;
    private final RegistrationProperties registration;
    private final SecurityPolicyService security;
    private final DeliveryService delivery;

    AuthController(AdminIdentityService identities, AdminTokenService tokens, TenantService tenants, AdminJwtProperties jwt, PublicRegistrationService registrations, RegistrationCodeService registrationCodes, RegistrationProperties registration, SecurityPolicyService security, DeliveryService delivery) {
        this.identities = identities;
        this.tokens = tokens;
        this.tenants = tenants;
        this.jwt = jwt;
        this.registrations = registrations;
        this.registrationCodes = registrationCodes;
        this.registration = registration;
        this.security = security;
        this.delivery = delivery;
    }

    @ManagementRouteClassification(ManagementRouteKind.AUTHENTICATION)
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody LoginRequest request, HttpServletRequest http) {
        if ("email".equals(request.loginType())) return emailLogin(request, http);
        if (!"password".equals(request.loginType()))
            throw new DomainException(ErrorCodeConstants.LOGIN_TYPE_UNSUPPORTED);
        var result = identities.authenticate(request.username(), request.password(), jwt.refreshTtl(), WebFramework.getUserAgent(http), http.getRemoteAddr());
        if (result.account().mfaEnabled()) {
            identities.logout(result.refreshToken());
            var challenge = security.issueTotpChallenge("admin", result.account().id(), null, "login");
            return ApiResponse.ok(LoginResponse.mfa(challenge.token(), challenge.expiresIn()));
        }
        return ApiResponse.ok(response(result.account(), result.refreshToken()));
    }

    @ManagementRouteClassification(ManagementRouteKind.AUTHENTICATION)
    @PostMapping("/mfa/verify")
    public ApiResponse<LoginResponse> verifyMfa(@RequestBody MfaLoginRequest request, HttpServletRequest http) {
        UUID account = security.consumeTotpChallenge(request.challengeToken(), request.code(), "admin", "login");
        var result = identities.completeMfa(account, jwt.refreshTtl(), WebFramework.getUserAgent(http), http.getRemoteAddr());
        return ApiResponse.ok(response(result.account(), result.refreshToken()));
    }

    @ManagementRouteClassification(ManagementRouteKind.AUTHENTICATION)
    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@RequestBody RegisterRequest request, HttpServletRequest http) {
        String username = request.username() == null || request.username().isBlank() ? request.email().split("@", 2)[0] : request.username();
        var result = registrations.register(username, request.email(), request.password(), request.code(), WebFramework.getUserAgent(http), http.getRemoteAddr());
        return ApiResponse.ok(response(result.identity().account(), result.identity().refreshToken()), "注册成功");
    }

    @ManagementRouteClassification(ManagementRouteKind.AUTHENTICATION)
    @PostMapping("/send-code")
    @Transactional
    public ApiResponse<Map<String, Object>> sendCode(@RequestBody SendCodeRequest request) {
        var body = new HashMap<String, Object>();
        if ("register".equals(request.type())) {
            var issued = registrationCodes.issue(request.email());
            delivery.enqueueEmail(null, issued.email(), "Easy1Auth 注册验证码", "您的验证码是 " + issued.code() + "，10分钟内有效。", "registration:" + issued.email() + ":" + java.time.Instant.now().getEpochSecond() / 60);
            if (registration.exposeCode()) body.put("code", issued.code());
        } else if ("login".equals(request.type())) {
            String challengeToken = security.decoyChallengeToken();
            var account = identities.activeAccountByEmail(request.email());
            if (account.isPresent()) {
                var challenge = security.issueEmailChallenge("admin", account.get().id(), null, "login", account.get().email());
                challengeToken = challenge.token();
                delivery.enqueueEmail(null, account.get().email(), "Easy1Auth 登录验证码", "您的登录验证码是 " + challenge.code() + "，10分钟内有效。", "email-login:" + account.get().id() + ":" + java.time.Instant.now().getEpochSecond() / 60);
                if (registration.exposeCode()) body.put("code", challenge.code());
            }
            body.put("challengeToken", challengeToken);
        } else {
            throw new DomainException(ErrorCodeConstants.CODE_TYPE_UNSUPPORTED);
        }
        return ApiResponse.ok(body, "验证码已进入发送队列");
    }

    @ManagementRouteClassification(ManagementRouteKind.AUTHENTICATION)
    @PostMapping("/refresh")
    public ApiResponse<Map<String, String>> refresh(@RequestBody RefreshRequest request, HttpServletRequest http) {
        var result = identities.rotate(request.refreshToken(), jwt.refreshTtl(), WebFramework.getUserAgent(http), http.getRemoteAddr());
        return ApiResponse.ok(Map.of("token", tokens.issue(result.account()), "refreshToken", result.replacementToken()));
    }

    @ManagementRouteClassification(ManagementRouteKind.AUTHENTICATION)
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody(required = false) RefreshRequest request, @AuthenticationPrincipal Jwt principal) {
        if (request != null && request.refreshToken() != null) identities.logout(request.refreshToken());
        else identities.logoutAllAndInvalidate(UUID.fromString(principal.getSubject()));
        return ApiResponse.ok(null, "退出成功");
    }

    private ApiResponse<LoginResponse> emailLogin(LoginRequest request, HttpServletRequest http) {
        var challenge = security.consumeEmailChallenge(request.challengeToken(), request.code(), "admin", "login");
        var account = identities.account(challenge.subjectId());
        if (challenge.destination() == null || !account.email().equalsIgnoreCase(challenge.destination()))
            throw new DomainException(ErrorCodeConstants.MFA_CHALLENGE_INVALID);
        if (account.mfaEnabled()) {
            var totp = security.issueTotpChallenge("admin", account.id(), null, "login");
            return ApiResponse.ok(LoginResponse.mfa(totp.token(), totp.expiresIn()));
        }
        var result = identities.completeMfa(account.id(), jwt.refreshTtl(), WebFramework.getUserAgent(http), http.getRemoteAddr());
        return ApiResponse.ok(response(result.account(), result.refreshToken()));
    }

    private LoginResponse response(AdminAccount account, String refresh) {
        var memberships = tenants.list(account.id());
        UUID current = account.lastTenantId();
        boolean currentAccessible = current != null && memberships.stream().map(t -> t.id()).anyMatch(current::equals);
        if (!currentAccessible) current = memberships.isEmpty() ? null : memberships.getFirst().id();
        return LoginResponse.success(tokens.issue(account), refresh, new LoginUser(account.id(), account.username(), account.email(), null, current), memberships);
    }

    public record LoginRequest(String username, String password, String email, String code, String challengeToken,
                               String loginType) {
    }

    public record RegisterRequest(String email, String password, String code, String username) {
    }

    public record RefreshRequest(String refreshToken) {
    }

    public record SendCodeRequest(String email, String type) {
    }

    public record MfaLoginRequest(String challengeToken, String code) {
    }

    public record LoginUser(UUID id, String username, String email, String avatar, UUID currentTenantId) {
    }

    public record LoginResponse(String status, String token, String refreshToken, LoginUser user, List<?> tenants,
                                String challengeToken, List<String> methods, Integer expiresIn) {
        static LoginResponse success(String token, String refresh, LoginUser user, List<?> tenants) {
            return new LoginResponse("success", token, refresh, user, tenants, null, null, null);
        }

        static LoginResponse mfa(String challenge, int expires) {
            return new LoginResponse("mfa_required", null, null, null, List.of(), challenge, List.of("totp"), expires);
        }
    }
}
