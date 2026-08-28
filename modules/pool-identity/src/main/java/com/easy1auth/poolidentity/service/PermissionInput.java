package com.easy1auth.poolidentity.service;
import java.util.*;
import java.time.*;
import com.easy1auth.poolidentity.PoolUserView;


public record PermissionInput(String code, String name, String description, String type, UUID parentId,
                                  String resource, String action) {
    }
