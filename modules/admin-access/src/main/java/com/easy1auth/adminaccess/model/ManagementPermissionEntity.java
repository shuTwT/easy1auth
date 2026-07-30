package com.easy1auth.adminaccess.model;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Table;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "management_permission")
public interface ManagementPermissionEntity {
    @Id
    String code();

    String type();

    String name();

    @Nullable
    @Column(name = "parent_code")
    String parentCode();

    String resource();

    String action();

    @Column(name = "sort_order")
    int sortOrder();

    boolean active();
}
