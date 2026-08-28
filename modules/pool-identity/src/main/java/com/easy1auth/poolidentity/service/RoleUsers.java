package com.easy1auth.poolidentity.service;
import java.util.*;
import java.time.*;
import com.easy1auth.poolidentity.PoolUserView;


public record RoleUsers(List<PoolUserView> users, int total) {
    }
