package com.easy1auth.useraccess.model;

import org.babyfish.jimmer.sql.*;

@Entity
@Table(name = "pool_user_role")
public interface UserRoleAssignmentEntity {
    @Id
    UserRoleAssignmentId id();
}
