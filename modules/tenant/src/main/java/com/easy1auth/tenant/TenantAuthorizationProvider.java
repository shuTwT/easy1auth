package com.easy1auth.tenant;

import com.easy1auth.tenant.dto.TenantAuthorization;
import com.easy1auth.tenant.dto.TenantAuthorizationRequest;

import java.util.UUID;

/**
 * 租户授权解析端口（由 admin-access 模块实现）。
 *
 * <p>作为端口（Port）将租户模块与外部仓储解耦：租户模块只依赖本接口，
 * 具体的账号状态校验与权限解析由接入方实现。</p>
 */
public interface TenantAuthorizationProvider {
    /** 判断账号是否为激活状态（用于租户上下文解析前的有效性校验）。 */
    boolean isActiveAccount(UUID accountId);

    /** 解析账号在指定租户下的授权结果（权限集合与套餐信息）。 */
    TenantAuthorization resolve(TenantAuthorizationRequest request);
}
