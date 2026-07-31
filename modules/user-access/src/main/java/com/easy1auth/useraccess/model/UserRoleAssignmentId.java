package com.easy1auth.useraccess.model;

import org.babyfish.jimmer.sql.*;

import java.util.UUID;

@Embeddable
public interface UserRoleAssignmentId {
    @Column(name = "tenant_id")
    UUID tenantId();

    @Column(name = "user_id")
    UUID userId();

    @Column(name = "role_id")
    UUID roleId();
}
