package com.easy1auth.adminidentity.service;

import com.easy1auth.adminidentity.*;
import com.easy1auth.adminidentity.constant.ErrorCodeConstants;
import com.easy1auth.adminidentity.dto.IssuedCode;
import com.easy1auth.adminidentity.repository.AdminIdentityRepository;
import com.easy1auth.adminidentity.util.AdminIdentityNormalizer;
import com.easy1auth.adminidentity.util.TokenHash;
import com.easy1auth.infrastructure.foundation.error.DomainException;
import com.easy1auth.infrastructure.foundation.id.UuidV7;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.*;

/**
 * 注册码服务：为管理后台注册流程签发邮箱验证码。
 *
 * <p>生成 6 位数字验证码，仅保存其 SHA-256 哈希，验证码有效期 10 分钟，
 * 用于注册时校验邮箱归属。</p>
 */
@Service
public class RegistrationCodeService {
    /** 安全随机数生成器（生成 6 位验证码） */
    private static final SecureRandom RANDOM = new SecureRandom();
    /** 管理账号身份数据访问仓储 */
    private final AdminIdentityRepository repository;
    private final RegistrationCodeNativeSql registrationCodes;

    RegistrationCodeService(AdminIdentityRepository repository, RegistrationCodeNativeSql registrationCodes) {
        this.repository = repository;
        this.registrationCodes = registrationCodes;
    }

    /**
     * 为指定邮箱签发注册码。
     *
     * @param email 目标邮箱
     * @return 签发结果（含明文验证码与过期时间，明文仅供本次返回使用）
     */
    @Transactional
    public IssuedCode issue(String email) {
        String normalizedEmail = AdminIdentityNormalizer.normalizeEmail(email);
        if (normalizedEmail == null || !normalizedEmail.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || normalizedEmail.length() > 320) {
            throw new DomainException(ErrorCodeConstants.EMAIL_INVALID);
        }
        if (repository.exists("__never__", normalizedEmail)) {
            throw new DomainException(ErrorCodeConstants.ADMIN_EXISTS_EMAIL);
        }
        String code = "%06d".formatted(RANDOM.nextInt(1_000_000));
        registrationCodes.issue(UuidV7.randomUuid(), normalizedEmail, TokenHash.sha256(code), Instant.now().plus(Duration.ofMinutes(10)));
        return new IssuedCode(normalizedEmail, code, Instant.now().plus(Duration.ofMinutes(10)));
    }

    
    
}
