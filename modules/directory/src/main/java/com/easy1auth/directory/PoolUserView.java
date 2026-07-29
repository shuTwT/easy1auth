package com.easy1auth.directory;
import java.time.Instant; import java.util.*;
public record PoolUserView(UUID id,UUID tenantId,String username,String email,String phone,String name,String avatar,String status,boolean emailVerified,boolean phoneVerified,String department,String position,Map<String,Object> customAttributes,Instant lastLoginAt,Instant createdAt,Instant updatedAt){}
