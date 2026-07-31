package com.easy1auth.adminaccess;

import java.util.Arrays;
import java.util.Optional;

public enum ManagementPermissionType {
    DIRECTORY("directory"),
    MENU("menu"),
    ACTION("action"),
    DATA("data");

    private final String databaseValue;

    ManagementPermissionType(String databaseValue) {
        this.databaseValue = databaseValue;
    }

    public String databaseValue() {
        return databaseValue;
    }

    public static Optional<ManagementPermissionType> fromDatabaseValue(String value) {
        return Arrays.stream(values()).filter(type -> type.databaseValue.equals(value)).findFirst();
    }
}
