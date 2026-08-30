package com.easy1auth.poolidentity.dto;
import java.util.*;

/** 角色创建或更新入参。 */
public record RoleInput(String name, String code, String description, String type, Map<String, Boolean> permissions,
                            String dataScope, UUID parentId) {
    }
