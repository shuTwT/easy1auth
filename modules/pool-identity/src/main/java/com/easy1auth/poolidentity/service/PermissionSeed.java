package com.easy1auth.poolidentity.service;
import java.util.*;
import java.time.*;
import com.easy1auth.poolidentity.PoolUserView;

/** 预置权限种子定义，用于首次访问租户权限目录时初始化权限。 */
record PermissionSeed(String code, String name, String type, String resource, String action) {
    }
