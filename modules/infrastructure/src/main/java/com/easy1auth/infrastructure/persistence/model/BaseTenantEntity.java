package com.easy1auth.infrastructure.persistence.model;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.MappedSuperclass;

import java.util.UUID;

/**
 * 租户隔离基类：所有需要按租户隔离数据的 jimmer 实体都继承该接口。
 *
 * <p>提供统一的 tenant_id 字段，配合租户上下文过滤器实现多租户数据隔离。</p>
 */
@MappedSuperclass
public interface BaseTenantEntity {
    /** 所属租户 ID */
    @Column(name = "tenant_id")
    UUID tenantId();
}
