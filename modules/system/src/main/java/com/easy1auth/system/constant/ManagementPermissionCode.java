package com.easy1auth.system.constant;

import java.util.Arrays;
import java.util.Optional;

/**
 * 管理端权限码枚举。
 *
 * <p>以 {@code 资源:动作}（如 "user:list"）为唯一标识，覆盖菜单、目录与操作三类
 * 权限。每个权限码同时声明其作用域（{@link ManagementPermissionScope}）与类型
 * （{@link ManagementPermissionType}），是管理端授权判断与权限目录校验的标准单位。</p>
 */
public enum ManagementPermissionCode {
    MENU_POOL_TENANT("menu:pool-tenant", ManagementPermissionScope.USER_POOL, ManagementPermissionType.MENU),
    POOL_TENANT_LIST("pool-tenant:list", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    POOL_TENANT_READ("pool-tenant:read", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    POOL_TENANT_CREATE("pool-tenant:create", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    POOL_TENANT_UPDATE("pool-tenant:update", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    POOL_TENANT_DELETE("pool-tenant:delete", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    MENU_DASHBOARD("menu:dashboard", ManagementPermissionScope.USER_POOL, ManagementPermissionType.MENU),
    DIRECTORY_USER_MANAGEMENT("menu:user-management", ManagementPermissionScope.USER_POOL, ManagementPermissionType.DIRECTORY),
    MENU_USER("menu:user", ManagementPermissionScope.USER_POOL, ManagementPermissionType.MENU),
    MENU_GROUP("menu:group", ManagementPermissionScope.USER_POOL, ManagementPermissionType.MENU),
    MENU_POSITION("menu:position", ManagementPermissionScope.USER_POOL, ManagementPermissionType.MENU),
    MENU_ROLE("menu:role", ManagementPermissionScope.USER_POOL, ManagementPermissionType.MENU),
    DIRECTORY_PERMISSION_MANAGEMENT("directory:permission-management", ManagementPermissionScope.USER_POOL, ManagementPermissionType.DIRECTORY),
    MENU_PERMISSION_SPACE("menu:permission-space", ManagementPermissionScope.USER_POOL, ManagementPermissionType.MENU),
    MENU_PERMISSION_DATA_RESOURCE("menu:permission-data-resource", ManagementPermissionScope.USER_POOL, ManagementPermissionType.MENU),
    MENU_ADMIN_USER("menu:admin-user", ManagementPermissionScope.PLATFORM, ManagementPermissionType.MENU),
    MENU_APPLICATION("menu:application", ManagementPermissionScope.USER_POOL, ManagementPermissionType.MENU),
    DIRECTORY_IDENTITY_SOURCE_MANAGEMENT("directory:identity-source-management", ManagementPermissionScope.USER_POOL, ManagementPermissionType.DIRECTORY),
    MENU_SOCIAL_IDENTITY_SOURCE("menu:social-identity-source", ManagementPermissionScope.USER_POOL, ManagementPermissionType.MENU),
    MENU_ENTERPRISE_IDENTITY_SOURCE("menu:enterprise-identity-source", ManagementPermissionScope.USER_POOL, ManagementPermissionType.MENU),
    MENU_SYNC_IDENTITY_SOURCE("menu:sync-identity-source", ManagementPermissionScope.USER_POOL, ManagementPermissionType.MENU),
    DIRECTORY_PLATFORM_MANAGEMENT("directory:platform-management", ManagementPermissionScope.PLATFORM, ManagementPermissionType.DIRECTORY),
    MENU_USER_POOL("menu:tenant", ManagementPermissionScope.PLATFORM, ManagementPermissionType.MENU),
    MENU_USER_POOL_PACKAGE("menu:tenant-package", ManagementPermissionScope.PLATFORM, ManagementPermissionType.MENU),
    MENU_MENU_MANAGEMENT("menu:menu-management", ManagementPermissionScope.PLATFORM, ManagementPermissionType.MENU),
    DIRECTORY_BRAND_MANAGEMENT("directory:brand-management", ManagementPermissionScope.USER_POOL, ManagementPermissionType.DIRECTORY),
    MENU_BRAND_LOGIN_STYLE("menu:brand-login-style", ManagementPermissionScope.USER_POOL, ManagementPermissionType.MENU),
    MENU_MESSAGE_SERVICE("menu:message-service", ManagementPermissionScope.USER_POOL, ManagementPermissionType.MENU),
    MENU_CUSTOM_DOMAIN("menu:custom-domain", ManagementPermissionScope.USER_POOL, ManagementPermissionType.MENU),
    MENU_SECURITY("menu:security", ManagementPermissionScope.USER_POOL, ManagementPermissionType.MENU),
    MENU_PERSONALIZATION("menu:personalization", ManagementPermissionScope.USER_POOL, ManagementPermissionType.MENU),
    MENU_AUDIT("menu:audit", ManagementPermissionScope.USER_POOL, ManagementPermissionType.MENU),
    ADMIN_USER_LIST("admin-user:list", ManagementPermissionScope.PLATFORM, ManagementPermissionType.ACTION),
    ADMIN_USER_STATS("admin-user:stats", ManagementPermissionScope.PLATFORM, ManagementPermissionType.ACTION),
    ADMIN_USER_READ("admin-user:read", ManagementPermissionScope.PLATFORM, ManagementPermissionType.ACTION),
    ADMIN_USER_CREATE("admin-user:create", ManagementPermissionScope.PLATFORM, ManagementPermissionType.ACTION),
    ADMIN_USER_UPDATE("admin-user:update", ManagementPermissionScope.PLATFORM, ManagementPermissionType.ACTION),
    ADMIN_USER_STATUS("admin-user:status", ManagementPermissionScope.PLATFORM, ManagementPermissionType.ACTION),
    ADMIN_USER_RESET_PASSWORD("admin-user:reset-password", ManagementPermissionScope.PLATFORM, ManagementPermissionType.ACTION),
    ADMIN_USER_RESET_MFA("admin-user:reset-mfa", ManagementPermissionScope.PLATFORM, ManagementPermissionType.ACTION),
    ADMIN_USER_ASSIGN_ROLE("admin-user:assign-role", ManagementPermissionScope.PLATFORM, ManagementPermissionType.ACTION),
    ADMIN_USER_REMOVE_USER_POOL("admin-user:remove-tenant", ManagementPermissionScope.PLATFORM, ManagementPermissionType.ACTION),
    USER_POOL_LIST("tenant:list", ManagementPermissionScope.PLATFORM, ManagementPermissionType.ACTION),
    USER_POOL_CREATE("tenant:create", ManagementPermissionScope.PLATFORM, ManagementPermissionType.ACTION),
    USER_POOL_READ("tenant:read", ManagementPermissionScope.PLATFORM, ManagementPermissionType.ACTION),
    USER_POOL_UPDATE("tenant:update", ManagementPermissionScope.PLATFORM, ManagementPermissionType.ACTION),
    USER_POOL_STATUS("tenant:status", ManagementPermissionScope.PLATFORM, ManagementPermissionType.ACTION),
    USER_POOL_DELETE("tenant:delete", ManagementPermissionScope.PLATFORM, ManagementPermissionType.ACTION),
    USER_POOL_OWNER_TRANSFER("tenant:owner-transfer", ManagementPermissionScope.PLATFORM, ManagementPermissionType.ACTION),
    USER_POOL_MEMBER_REMOVE("tenant:member-remove", ManagementPermissionScope.PLATFORM, ManagementPermissionType.ACTION),
    MENU_MANAGEMENT_LIST("menu-management:list", ManagementPermissionScope.PLATFORM, ManagementPermissionType.ACTION),
    USER_POOL_PACKAGE_PERMISSION_CATALOG("tenant-package:permissions-catalog", ManagementPermissionScope.PLATFORM, ManagementPermissionType.ACTION),
    USER_POOL_PACKAGE_LIST("tenant-package:list", ManagementPermissionScope.PLATFORM, ManagementPermissionType.ACTION),
    USER_POOL_PACKAGE_READ("tenant-package:read", ManagementPermissionScope.PLATFORM, ManagementPermissionType.ACTION),
    USER_POOL_PACKAGE_CREATE("tenant-package:create", ManagementPermissionScope.PLATFORM, ManagementPermissionType.ACTION),
    USER_POOL_PACKAGE_UPDATE("tenant-package:update", ManagementPermissionScope.PLATFORM, ManagementPermissionType.ACTION),
    USER_POOL_PACKAGE_STATUS("tenant-package:status", ManagementPermissionScope.PLATFORM, ManagementPermissionType.ACTION),
    USER_POOL_PACKAGE_PERMISSION_REPLACE("tenant-package:permissions-replace", ManagementPermissionScope.PLATFORM, ManagementPermissionType.ACTION),
    USER_POOL_PACKAGE_DELETE("tenant-package:delete", ManagementPermissionScope.PLATFORM, ManagementPermissionType.ACTION),
    SECURITY_PASSWORD_POLICY_READ("security:password-policy:read", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    SECURITY_PASSWORD_CHANGE("security:password:change", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    SECURITY_MFA_READ("security:mfa:read", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    SECURITY_MFA_SETUP("security:mfa:setup", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    SECURITY_MFA_ENABLE("security:mfa:enable", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    SECURITY_MFA_DISABLE("security:mfa:disable", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    SECURITY_MFA_VERIFY("security:mfa:verify", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    SECURITY_MFA_EMAIL_CODE("security:mfa:email-code", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    APPLICATION_LIST("application:list", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    APPLICATION_STATS("application:stats", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    APPLICATION_READ("application:read", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    APPLICATION_CREATE("application:create", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    APPLICATION_UPDATE("application:update", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    APPLICATION_DELETE("application:delete", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    APPLICATION_STATUS("application:status", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    APPLICATION_REGENERATE_SECRET("application:regenerate-secret", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    SOCIAL_IDENTITY_SOURCE_LIST("social-identity-source:list", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    SOCIAL_IDENTITY_SOURCE_STATS("social-identity-source:stats", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    SOCIAL_IDENTITY_SOURCE_READ("social-identity-source:read", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    SOCIAL_IDENTITY_SOURCE_CREATE("social-identity-source:create", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    SOCIAL_IDENTITY_SOURCE_UPDATE("social-identity-source:update", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    SOCIAL_IDENTITY_SOURCE_DELETE("social-identity-source:delete", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    ENTERPRISE_IDENTITY_SOURCE_LIST("enterprise-identity-source:list", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    ENTERPRISE_IDENTITY_SOURCE_STATS("enterprise-identity-source:stats", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    ENTERPRISE_IDENTITY_SOURCE_READ("enterprise-identity-source:read", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    ENTERPRISE_IDENTITY_SOURCE_CREATE("enterprise-identity-source:create", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    ENTERPRISE_IDENTITY_SOURCE_UPDATE("enterprise-identity-source:update", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    ENTERPRISE_IDENTITY_SOURCE_DELETE("enterprise-identity-source:delete", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    ENTERPRISE_IDENTITY_SOURCE_SYNC("enterprise-identity-source:sync", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    AUDIT_LIST("audit:list", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    AUDIT_STATS("audit:stats", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    AUDIT_READ("audit:read", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    AUDIT_CLEANUP("audit:cleanup", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    ADMIN_ROLE_STATS("admin-role:stats", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    ADMIN_ROLE_PERMISSIONS_CATALOG("admin-role:permissions-catalog", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    ADMIN_ROLE_LIST("admin-role:list", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    ADMIN_ROLE_READ("admin-role:read", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    ADMIN_ROLE_CREATE("admin-role:create", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    ADMIN_ROLE_UPDATE("admin-role:update", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    ADMIN_ROLE_DELETE("admin-role:delete", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    ADMIN_ROLE_ASSIGN("admin-role:assign", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    USER_LIST("user:list", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    USER_STATS("user:stats", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    USER_READ("user:read", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    USER_CREATE("user:create", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    USER_UPDATE("user:update", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    USER_DELETE("user:delete", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    USER_STATUS("user:status", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    USER_RESET_PASSWORD("user:reset-password", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    USER_CHANGE_PASSWORD("user:change-password", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    USER_ROLES_READ("user:roles:read", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    USER_ROLES_ASSIGN("user:roles:assign", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    USER_GROUPS_READ("user:groups:read", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    USER_GROUPS_ASSIGN("user:groups:assign", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    USER_IMPORT_TEMPLATE("user:import:template", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    USER_IMPORT("user:import", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    USER_ROLE_LIST("user-role:list", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    USER_ROLE_STATS("user-role:stats", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    USER_ROLE_TREE("user-role:tree", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    USER_ROLE_READ("user-role:read", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    USER_ROLE_CREATE("user-role:create", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    USER_ROLE_UPDATE("user-role:update", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    USER_ROLE_DELETE("user-role:delete", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    USER_ROLE_USERS_READ("user-role:users:read", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    USER_ROLE_USERS_ASSIGN("user-role:users:assign", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    USER_ROLE_USERS_REMOVE("user-role:users:remove", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    USER_ROLE_USER_ROLES_READ("user-role:user-roles:read", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    USER_ROLE_USER_ROLES_REPLACE("user-role:user-roles:replace", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    DATA_PERMISSION_LIST("data-permission:list", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    DATA_PERMISSION_STATS("data-permission:stats", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    DATA_PERMISSION_TREE("data-permission:tree", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    DATA_PERMISSION_READ("data-permission:read", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    DATA_PERMISSION_CREATE("data-permission:create", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    DATA_PERMISSION_UPDATE("data-permission:update", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    DATA_PERMISSION_DELETE("data-permission:delete", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    PERMISSION_SPACE_LIST("permission-space:list", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    PERMISSION_SPACE_CREATE("permission-space:create", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    PERMISSION_SPACE_UPDATE("permission-space:update", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    PERMISSION_SPACE_DELETE("permission-space:delete", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    GROUP_LIST("group:list", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    GROUP_STATS("group:stats", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    GROUP_READ("group:read", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    GROUP_CREATE("group:create", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    GROUP_UPDATE("group:update", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    GROUP_DELETE("group:delete", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    GROUP_MEMBERS_READ("group:members:read", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    GROUP_MEMBERS_ADD("group:members:add", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    GROUP_MEMBERS_REMOVE("group:members:remove", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    POSITION_LIST("position:list", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    POSITION_STATS("position:stats", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    POSITION_READ("position:read", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    POSITION_CREATE("position:create", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    POSITION_UPDATE("position:update", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    POSITION_DELETE("position:delete", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    WEBHOOK_LIST("webhook:list", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    WEBHOOK_CREATE("webhook:create", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    WEBHOOK_UPDATE("webhook:update", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    WEBHOOK_ROTATE_SECRET("webhook:rotate-secret", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    WEBHOOK_DELETE("webhook:delete", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    SECURITY_POLICY_READ("security-policy:read", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    SECURITY_POLICY_UPDATE("security-policy:update", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    LOGIN_STYLE_READ("login-style:read", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    LOGIN_STYLE_UPDATE("login-style:update", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    CUSTOM_DOMAIN_LIST("custom-domain:list", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    CUSTOM_DOMAIN_CREATE("custom-domain:create", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    CUSTOM_DOMAIN_VERIFY("custom-domain:verify", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    CUSTOM_DOMAIN_SSL("custom-domain:ssl", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    CUSTOM_DOMAIN_DELETE("custom-domain:delete", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    MESSAGE_TEMPLATE_LIST("message-template:list", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    MESSAGE_TEMPLATE_CREATE("message-template:create", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    MESSAGE_TEMPLATE_UPDATE("message-template:update", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION),
    MESSAGE_TEMPLATE_DELETE("message-template:delete", ManagementPermissionScope.USER_POOL, ManagementPermissionType.ACTION);

    /** 权限码字符串（如 "user:list"，与管理端权限目录中的 code 一致） */
    private final String value;
    /** 权限作用域（平台 / 用户池） */
    private final ManagementPermissionScope scope;
    /** 权限类型（菜单 / 目录 / 操作） */
    private final ManagementPermissionType type;

    ManagementPermissionCode(String value, ManagementPermissionScope scope, ManagementPermissionType type) {
        this.value = value;
        this.scope = scope;
        this.type = type;
    }

    /** 返回权限码字符串。 */
    public String value() {
        return value;
    }

    /** 返回权限作用域。 */
    public ManagementPermissionScope scope() {
        return scope;
    }

    /** 返回权限类型。 */
    public ManagementPermissionType type() {
        return type;
    }

    /** 按权限码字符串反查枚举，未找到时返回空 Optional。 */
    public static Optional<ManagementPermissionCode> fromValue(String value) {
        return Arrays.stream(values()).filter(code -> code.value.equals(value)).findFirst();
    }
}
