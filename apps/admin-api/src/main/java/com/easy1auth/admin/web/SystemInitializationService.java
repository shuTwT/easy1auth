package com.easy1auth.admin.web;

import com.easy1auth.framework.common.error.DomainException;
import com.easy1auth.system.constant.ErrorCodeConstants;
import com.easy1auth.system.dto.AdminAccount;
import com.easy1auth.system.service.AdminIdentityService;
import com.easy1auth.system.service.SystemConfigService;
import com.easy1auth.tenant.service.UserPoolService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** 编排首个系统管理员、系统用户池及初始化状态的一次性初始化流程。 */
@Service
class SystemInitializationService {
    /** 管理员身份服务。 */
    private final AdminIdentityService identities;
    /** 用户池服务。 */
    private final UserPoolService userPools;
    /** 系统配置服务。 */
    private final SystemConfigService systemConfig;

    SystemInitializationService(AdminIdentityService identities, UserPoolService userPools,
                                SystemConfigService systemConfig) {
        this.identities = identities;
        this.userPools = userPools;
        this.systemConfig = systemConfig;
    }

    /** 原子完成系统初始化；初始化状态行锁保证并发请求中只有一个成功。 */
    @Transactional
    void initialize(String account, String password) {
        if (systemConfig.lockAndIsInitialized()) {
            throw new DomainException(ErrorCodeConstants.SYSTEM_ALREADY_INITIALIZED);
        }
        String email = account == null ? null : account.strip().toLowerCase(Locale.ROOT);
        String username = username(email);
        AdminAccount administrator = identities.createAdministrator(username, email, password);
        userPools.createSystem(administrator.id());
        systemConfig.markInitialized();
    }

    /** 从邮箱本地部分生成首个管理员用户名。 */
    private static String username(String email) {
        if (email == null) {
            return null;
        }
        int separator = email.indexOf('@');
        if (separator <= 0) {
            return email;
        }
        return email.substring(0, Math.min(separator, 100));
    }
}
