package com.easy1auth.adminidentity;

import com.easy1auth.foundation.error.DomainException;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.easy1auth.security.*;

class AdminIdentityServiceTest {
    private final AdminIdentityRepository repository=mock(AdminIdentityRepository.class);
    private final PasswordEncoder passwords=mock(PasswordEncoder.class);
    private final LoginProtectionService protection=mock(LoginProtectionService.class); private final SecurityPolicyService security=mock(SecurityPolicyService.class); private final AdminIdentityService service=new AdminIdentityService(repository,passwords,protection,security);
    private final AdminAccount account=new AdminAccount(UUID.randomUUID(),"admin","admin@example.com",null,"active",3,null,false,null,null,Instant.now(),Instant.now());

    @Test void loginUsesGenericFailureForUnknownAccount(){assertThatThrownBy(()->service.authenticate("missing","wrong",Duration.ofDays(30),null,null)).isInstanceOf(DomainException.class).extracting("code").isEqualTo("INVALID_CREDENTIALS");}
    @Test void loginIssuesOpaqueRefreshSessionAfterPasswordCheck(){when(repository.findCredential("admin")).thenReturn(Optional.of(new AdminIdentityRepository.CredentialRow(account,"hash")));when(passwords.matches("correct","hash")).thenReturn(true);var result=service.authenticate("admin","correct",Duration.ofDays(30),"agent","ip");assertThat(result.refreshToken()).doesNotContain(".").hasSizeGreaterThan(40);verify(repository).createSession(any(),eq(account.id()),matches("[0-9a-f]{64}"),eq(3L),any(),eq("agent"),eq("ip"));}
    @Test void staleRefreshSecurityVersionIsRejected(){var row=new AdminIdentityRepository.SessionRow(UUID.randomUUID(),2,account);when(repository.lockSession(anyString())).thenReturn(Optional.of(row));assertThatThrownBy(()->service.rotate("token",Duration.ofDays(30),null,null)).isInstanceOf(DomainException.class).extracting("code").isEqualTo("INVALID_REFRESH_TOKEN");}
}
