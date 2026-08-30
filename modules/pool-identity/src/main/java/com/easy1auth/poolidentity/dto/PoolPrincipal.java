package com.easy1auth.poolidentity.dto;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
/** 认证通过后的 pool_user 身份凭据。 */
public record PoolPrincipal(UUID id, UUID tenantId, String username, String name, @Nullable String email) { }
