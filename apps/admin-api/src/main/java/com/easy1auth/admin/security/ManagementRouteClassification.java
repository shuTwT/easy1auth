package com.easy1auth.admin.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 管理路由分类注解（标注在方法上）。
 *
 * <p>用于显式声明某管理接口的路由分类（见 {@link ManagementRouteKind}），适用于既不需要
 * 租户权限也不需要平台权限的接口（如公开、登录、自助服务、授权上下文类路由）。
 * 启动时由 {@link ManagementRouteInventory} 校验分类与控制器、白名单是否匹配。</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface ManagementRouteClassification {
    /** 声明的路由分类 */
    ManagementRouteKind value();
}
