package com.easy1auth.tenant;

import com.easy1auth.tenant.dto.UserPoolAuthorization;
import com.easy1auth.tenant.dto.UserPoolAuthorizationRequest;

import java.util.UUID;

/**
 * 用户池授权解析端口（由 system 模块实现）。
 *
 * <p>作为端口（Port）将用户池模块与外部仓储解耦：用户池模块只依赖本接口，
 * 具体的账号状态校验与权限解析由接入方实现。</p>
 */
public interface UserPoolAuthorizationProvider {
    /** 判断账号是否为激活状态（用于用户池上下文解析前的有效性校验）。 */
    boolean isActiveAccount(UUID accountId);

    /** 解析账号在指定用户池下的授权结果（权限集合与套餐信息）。 */
    UserPoolAuthorization resolve(UserPoolAuthorizationRequest request);
}
