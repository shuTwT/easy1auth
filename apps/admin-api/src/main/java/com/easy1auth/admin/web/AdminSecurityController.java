package com.easy1auth.admin.web;
import com.easy1auth.adminidentity.*; import com.easy1auth.audit.DeliveryService; import com.easy1auth.security.SecurityPolicyService;
import org.springframework.security.core.annotation.AuthenticationPrincipal; import org.springframework.security.oauth2.jwt.Jwt; import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/security") public class AdminSecurityController {
 private final AdminIdentityService identities; private final SecurityPolicyService security; private final DeliveryService delivery;
 AdminSecurityController(AdminIdentityService identities,SecurityPolicyService security,DeliveryService delivery){this.identities=identities;this.security=security;this.delivery=delivery;}
 @GetMapping("/password-policy") public Map<String,Object> policy(@AuthenticationPrincipal Jwt jwt){return Map.of("status","success","policy",SecurityPolicyService.adminPolicy(),"expiryStatus",Map.of("expired",false,"daysUntilExpiry",90));}
 @PostMapping("/change-password") public Map<String,String> change(@AuthenticationPrincipal Jwt jwt,@RequestBody ChangePassword input){if(!Objects.equals(input.newPassword(),input.confirmPassword()))throw new com.easy1auth.foundation.error.DomainException("PASSWORD_CONFIRM_MISMATCH","两次输入的密码不一致",400);security.validatePassword(input.newPassword(),SecurityPolicyService.adminPolicy());identities.changePassword(id(jwt),input.currentPassword(),input.newPassword());return Map.of("status","success","message","密码修改成功，请重新登录");}
 @GetMapping("/mfa/status") public Map<String,Object> status(@AuthenticationPrincipal Jwt jwt){var status=security.status("admin",id(jwt));return Map.of("status","success","mfa",Map.of("enabled",status.enabled(),"type",status.methods().isEmpty()?"":status.methods().getFirst()));}
 @PostMapping("/mfa/setup") public Map<String,Object> setup(@AuthenticationPrincipal Jwt jwt){var account=identities.account(id(jwt));return Map.of("status","success","data",security.setupTotp("admin",account.id(),null,account.email()));}
 @PostMapping("/mfa/enable") public Map<String,String> enable(@AuthenticationPrincipal Jwt jwt,@RequestBody TokenInput in){security.enableTotp("admin",id(jwt),in.token());identities.enableMfa(id(jwt),"totp");return ok("MFA 已启用，请重新登录");}
 @PostMapping("/mfa/disable") public Map<String,String> disable(@AuthenticationPrincipal Jwt jwt,@RequestBody TokenInput in){security.disable("admin",id(jwt),in.token());identities.resetMfa(id(jwt));return ok("MFA 已禁用，请重新登录");}
 @PostMapping("/mfa/verify") public Map<String,String> verify(@AuthenticationPrincipal Jwt jwt,@RequestBody TokenInput in){security.verifyTotp("admin",id(jwt),in.token());return ok("MFA 验证成功");}
 @PostMapping("/mfa/send-email-code") public Map<String,Object> email(@AuthenticationPrincipal Jwt jwt){var account=identities.account(id(jwt));var c=security.issueEmailChallenge("admin",account.id(),null,"step_up");delivery.enqueueEmail(null,account.email(),"Easy1Auth 安全验证码","您的验证码是 "+c.code()+"，10分钟内有效。","mfa-email:"+c.token());return Map.of("status","success","message","验证码已发送","challengeToken",c.token());}
 private static UUID id(Jwt jwt){return UUID.fromString(jwt.getSubject());} private static Map<String,String> ok(String message){return Map.of("status","success","message",message);}
 public record ChangePassword(String currentPassword,String newPassword,String confirmPassword){} public record TokenInput(String token,String challengeToken,String type){}
}
