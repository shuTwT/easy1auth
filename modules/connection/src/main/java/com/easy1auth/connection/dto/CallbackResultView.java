package com.easy1auth.connection.dto;

import java.util.UUID;
/** 社交授权回调处理结果。 */
public record CallbackResultView(UUID tenantId, UUID sourceId, UUID poolUserId, PendingIdentity identity) { }
