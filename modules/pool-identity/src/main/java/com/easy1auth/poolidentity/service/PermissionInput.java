package com.easy1auth.poolidentity.service;
import java.util.*;
import java.time.*;
import com.easy1auth.poolidentity.PoolUserView;

/** 权限创建或更新入参。 */
public record PermissionInput(String code, String name, String description, String type, UUID parentId,
                                  String resource, String action) {
    }
