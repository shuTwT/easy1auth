package com.easy1auth.social.service;
import java.util.UUID;
/** 社交授权回调处理结果。 */
public record CallbackResult(UUID tenantId, UUID sourceId, UUID poolUserId, PendingIdentity identity) { }
