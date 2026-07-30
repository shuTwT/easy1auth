package com.easy1auth.tenant;

public enum TenantDataBoundary {
    NONE("none"),
    PLATFORM_ALL("data:platform:all"),
    TENANT_ALL("data:tenant:all");

    private final String code;

    TenantDataBoundary(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
