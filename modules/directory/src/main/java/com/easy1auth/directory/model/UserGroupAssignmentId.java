package com.easy1auth.directory.model;
import org.babyfish.jimmer.sql.*;import java.util.UUID;
@Embeddable public interface UserGroupAssignmentId{@Column(name="tenant_id")UUID tenantId();@Column(name="user_id")UUID userId();@Column(name="group_id")UUID groupId();}
