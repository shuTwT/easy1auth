package com.easy1auth.admin.web;

import com.easy1auth.admin.config.AdminJwtProperties;
import com.easy1auth.adminidentity.service.AdminIdentityService;
import com.easy1auth.adminidentity.AuthenticatedAdmin;
import com.easy1auth.tenant.service.TenantService;
import com.easy1auth.tenant.dto.TenantSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 公共注册服务。
 *
 * <p>支撑管理端开放注册流程：创建管理账号并为其自动创建默认套餐的租户，
 * 使新注册的管理员立刻获得一个可用租户。注册与建租户在同一事务内完成。</p>
 */
@Service
class PublicRegistrationService {
    /** 管理账号身份服务 */
    private final AdminIdentityService identities;
    /** 租户服务（用于为新账号创建默认租户） */
    private final TenantService tenants;
    /** 管理端 JWT 配置 */
    private final AdminJwtProperties jwt;

    PublicRegistrationService(AdminIdentityService identities, TenantService tenants, AdminJwtProperties jwt) {
        this.identities = identities;
        this.tenants = tenants;
        this.jwt = jwt;
    }

    /** 注册管理账号并创建其默认租户，返回注册结果（账号身份 + 新租户摘要）。 */
    @Transactional
    RegistrationResult register(String username, String email, String password, String code, String userAgent, String ip) {
        AuthenticatedAdmin identity = identities.register(username, email, password, code, jwt.refreshTtl(), userAgent, ip);
        TenantSummary tenant = tenants.createOrdinaryWithDefaultPackage(username + "的租户", identity.account().id());
        return new RegistrationResult(identity, tenant);
    }

    /**
     * 注册结果。
     *
     * @param identity 注册成功的管理账号身份（含访问凭证）
     * @param tenant   为新账号自动创建的默认租户摘要
     */
    
}
