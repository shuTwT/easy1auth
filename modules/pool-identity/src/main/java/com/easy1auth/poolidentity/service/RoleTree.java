package com.easy1auth.poolidentity.service;
import java.util.*;
import java.time.*;
import com.easy1auth.poolidentity.PoolUserView;


public record RoleTree(UUID id, String name, String code, String description, String type, long userCount,
                           List<RoleTree> children) {
    }
