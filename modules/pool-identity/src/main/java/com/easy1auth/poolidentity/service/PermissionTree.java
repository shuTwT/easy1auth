package com.easy1auth.poolidentity.service;
import java.util.*;
import java.time.*;
import com.easy1auth.poolidentity.PoolUserView;


public record PermissionTree(UUID id, String code, String name, String description, String type, String resource,
                                 String action, List<PermissionTree> children) {
    }
