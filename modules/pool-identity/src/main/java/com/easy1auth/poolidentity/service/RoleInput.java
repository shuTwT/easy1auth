package com.easy1auth.poolidentity.service;
import java.util.*;
import java.time.*;
import com.easy1auth.poolidentity.PoolUserView;


public record RoleInput(String name, String code, String description, String type, Map<String, Boolean> permissions,
                            String dataScope, UUID parentId) {
    }
