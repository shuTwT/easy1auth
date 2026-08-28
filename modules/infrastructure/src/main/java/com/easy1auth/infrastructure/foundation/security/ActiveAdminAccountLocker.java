package com.easy1auth.infrastructure.foundation.security;

import java.util.UUID;

/**
 * 管理账号锁定器接口：在执行涉及指定账号的变更操作前，
 * 加锁锁定账号，防止并发操作正在被变更的管理账号。
 */
public interface ActiveAdminAccountLocker {
    /** 锁定指定管理账号（须处于激活状态），否则抛出领域异常。 */
    void lockActive(UUID accountId);
}
