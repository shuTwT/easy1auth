package com.easy1auth.tenant.dto;

import java.util.UUID;

/** 成员关系状态视图。 */
public record MembershipState(UUID id, String role, boolean system, Long packageId) {
}
