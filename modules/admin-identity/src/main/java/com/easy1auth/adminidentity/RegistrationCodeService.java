package com.easy1auth.adminidentity;

import com.easy1auth.foundation.error.DomainException;
import com.easy1auth.foundation.id.UuidV7;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.*;

@Service
public class RegistrationCodeService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private final AdminIdentityRepository repository;

    RegistrationCodeService(AdminIdentityRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public IssuedCode issue(String email) {
        String normalizedEmail = AdminIdentityNormalizer.normalizeEmail(email);
        if (normalizedEmail == null || !normalizedEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || normalizedEmail.length() > 320)
            throw new DomainException(ErrorCodeConstants.EMAIL_INVALID);
        if (repository.exists("__never__", normalizedEmail))
            throw new DomainException(ErrorCodeConstants.ADMIN_EXISTS_EMAIL);
        String code = "%06d".formatted(RANDOM.nextInt(1_000_000));
        repository.issueRegistrationCode(UuidV7.randomUuid(), normalizedEmail, TokenHash.sha256(code), Instant.now().plus(Duration.ofMinutes(10)));
        return new IssuedCode(normalizedEmail, code, Instant.now().plus(Duration.ofMinutes(10)));
    }

    public record IssuedCode(String email, String code, Instant expiresAt) {
    }
}
