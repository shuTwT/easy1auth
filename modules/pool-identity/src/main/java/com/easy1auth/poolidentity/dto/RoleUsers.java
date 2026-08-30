package com.easy1auth.poolidentity.dto;
import java.util.*;


public record RoleUsers(List<PoolUserView> users, int total) {
    }
