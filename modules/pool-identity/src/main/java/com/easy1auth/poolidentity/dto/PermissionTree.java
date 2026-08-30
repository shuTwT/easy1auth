package com.easy1auth.poolidentity.dto;
import java.util.*;

/** 权限树节点视图。 */
public record PermissionTree(UUID id, String code, String name, String description, String type, String resource,
                                 String action, List<PermissionTree> children) {
    }
