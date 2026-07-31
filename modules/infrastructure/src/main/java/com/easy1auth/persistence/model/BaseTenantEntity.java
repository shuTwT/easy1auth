package com.easy1auth.persistence.model;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.MappedSuperclass;

import java.util.UUID;

@MappedSuperclass
public interface BaseTenantEntity {
    @Column(name = "tenant_id")
    UUID tenantId();
}
