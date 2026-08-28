package com.easy1auth.poolidentity.service;
import java.util.*;
import java.time.*;
import com.easy1auth.poolidentity.PoolUserView;

/** 角色树节点视图。 */
public record RoleTree(UUID id, String name, String code, String description, String type, long userCount,
                           List<RoleTree> children) {
    }
