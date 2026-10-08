package com.easy1auth.admin.web;

import com.easy1auth.admin.web.dto.RegistrationResult;
import com.easy1auth.admin.config.AdminJwtProperties;
import com.easy1auth.system.service.AdminIdentityService;
import com.easy1auth.system.dto.AuthenticatedAdmin;
import com.easy1auth.tenant.service.UserPoolService;
import com.easy1auth.tenant.dto.UserPoolSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 公共注册服务。
 *
 * <p>支撑管理端开放注册流程：创建管理账号并为其自动创建默认套餐的用户池，
 * 使新注册的管理员立刻获得一个可用用户池。注册与建用户池在同一事务内完成。</p>
 */
@Service
class PublicRegistrationService {
    /** 管理账号身份服务 */
    private final AdminIdentityService identities;
    /** 用户池服务（用于为新账号创建默认用户池） */
    private final UserPoolService userPools;
    /** 管理端 JWT 配置 */
    private final AdminJwtProperties jwt;

    PublicRegistrationService(AdminIdentityService identities, UserPoolService userPools, AdminJwtProperties jwt) {
        this.identities = identities;
        this.userPools = userPools;
        this.jwt = jwt;
    }

    /** 注册管理账号并创建其默认用户池，返回注册结果（账号身份 + 新用户池摘要）。 */
    @Transactional
    RegistrationResult register(String username, String email, String password, String code, String userAgent, String ip) {
        AuthenticatedAdmin identity = identities.register(username, email, password, code, jwt.refreshTtl(), userAgent, ip);
        UserPoolSummary tenant = userPools.createOrdinaryWithDefaultPackage(username + "的用户池", identity.account().id());
        return new RegistrationResult(identity, tenant);
    }

}
