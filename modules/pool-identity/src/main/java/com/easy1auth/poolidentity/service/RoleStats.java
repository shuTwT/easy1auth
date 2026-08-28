package com.easy1auth.poolidentity.service;
import java.util.*;
import java.time.*;
import com.easy1auth.poolidentity.PoolUserView;


public record RoleStats(long totalRoles, long systemRoles, long customRoles, long totalUsers) {
    }
