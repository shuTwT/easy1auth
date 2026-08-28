package com.easy1auth.poolidentity.service;
import java.util.*;
import java.time.*;
import com.easy1auth.poolidentity.PoolUserView;

/** 角色统计视图。 */
public record RoleStats(long totalRoles, long systemRoles, long customRoles, long totalUsers) {
    }
