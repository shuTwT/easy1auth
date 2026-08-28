package com.easy1auth.tenant.repository;

import java.util.UUID;

/** 成员关系状态视图。 */
public record MembershipState(UUID id, String role, boolean system, Long packageId) {
}
