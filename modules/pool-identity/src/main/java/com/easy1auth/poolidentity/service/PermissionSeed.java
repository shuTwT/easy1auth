package com.easy1auth.poolidentity.service;
import java.util.*;
import java.time.*;
import com.easy1auth.poolidentity.PoolUserView;


record PermissionSeed(String code, String name, String type, String resource, String action) {
    }
