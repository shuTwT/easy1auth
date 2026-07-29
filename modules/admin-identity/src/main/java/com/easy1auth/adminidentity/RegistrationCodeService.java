package com.easy1auth.adminidentity;
import com.easy1auth.foundation.error.DomainException; import com.easy1auth.foundation.id.UuidV7; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.security.SecureRandom; import java.time.*;
@Service public class RegistrationCodeService{
 private static final SecureRandom RANDOM=new SecureRandom(); private final AdminIdentityRepository repository;
 RegistrationCodeService(AdminIdentityRepository repository){this.repository=repository;}
 @Transactional public IssuedCode issue(String email){if(email==null||!email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")||email.length()>320)throw new DomainException("EMAIL_INVALID","邮箱格式不正确",400);if(repository.exists("__never__",email))throw new DomainException("ADMIN_EXISTS","该邮箱已被注册",409);String code="%06d".formatted(RANDOM.nextInt(1_000_000));repository.issueRegistrationCode(UuidV7.randomUuid(),email.toLowerCase(),TokenHash.sha256(code),Instant.now().plus(Duration.ofMinutes(10)));return new IssuedCode(email.toLowerCase(),code,Instant.now().plus(Duration.ofMinutes(10)));}
 public record IssuedCode(String email,String code,Instant expiresAt){}
}
