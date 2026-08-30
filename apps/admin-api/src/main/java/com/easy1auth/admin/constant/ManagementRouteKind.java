package com.easy1auth.admin.constant;

import com.easy1auth.admin.annotation.ManagementRouteClassification;
import com.easy1auth.admin.security.ManagementRouteInventory;

/**
 * 管理路由分类枚举。
 *
 * <p>描述管理端接口的访问性质，供 {@link ManagementRouteClassification} 注解与
 * {@link ManagementRouteInventory} 启动校验使用。</p>
 */
public enum ManagementRouteKind {
    /** 完全公开的路由，无需认证，且必须位于公开路由白名单中 */
    PUBLIC,
    /** 仅需登录认证即可访问的路由（认证控制器） */
    AUTHENTICATION,
    /** 已认证用户操作自身账号的路由（自助服务） */
    AUTHENTICATED_SELF,
    /** 需要租户上下文的路由（授权上下文控制器） */
    AUTHORIZATION_CONTEXT,
    /** 预留的待办路由分类（第 7 期租户控制面） */
    DEFERRED_TODO_7
}
