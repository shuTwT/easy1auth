package com.easy1auth.adminaccess;
import java.time.Instant; import java.util.*;
public record AdminMemberView(UUID id,UUID tenantId,String tenantRole,UUID currentTenantId,String username,String email,String phone,String status,boolean mfaEnabled,String mfaType,Instant lastLoginAt,Instant createdAt,Instant updatedAt,List<AdminRoleView> roles){}
