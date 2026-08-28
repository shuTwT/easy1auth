package com.easy1auth.poolidentity.service;
import java.util.*;
import java.time.*;
import com.easy1auth.poolidentity.PoolUserView;

/** 权限统计视图。 */
public record PermissionStats(long totalPermissions, long menuPermissions, long operationPermissions, long dataPermissions) {
    }
