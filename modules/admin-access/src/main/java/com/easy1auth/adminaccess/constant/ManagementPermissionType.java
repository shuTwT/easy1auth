package com.easy1auth.adminaccess.constant;

import java.util.Arrays;
import java.util.Optional;

/**
 * 管理端权限类型枚举。
 *
 * <p>描述管理端权限的三种形态：menu（菜单节点）、directory（目录分组）、
 * action（具体操作）。{@code databaseValue} 为持久化到数据库的取值，在
 * management_permission 表的 type 字段中使用。</p>
 */
public enum ManagementPermissionType {
    DIRECTORY("directory"),
    MENU("menu"),
    ACTION("action");

    /** 数据库存储值 */
    private final String databaseValue;

    ManagementPermissionType(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    /** 返回数据库存储值。 */
    public String databaseValue() {
        return databaseValue;
    }

    /** 按数据库存储值反查枚举，未匹配时返回空 Optional。 */
    public static Optional<ManagementPermissionType> fromDatabaseValue(String value) {
        return Arrays.stream(values()).filter(type -> type.databaseValue.equals(value)).findFirst();
    }
}
