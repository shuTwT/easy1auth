package com.easy1auth.framework.tenant.persistence.model;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.MappedSuperclass;

import java.util.UUID;

/**
 * 用户池隔离基类：所有需要按用户池隔离数据的 jimmer 实体都继承该接口。
 *
 * <p>提供统一的 tenant_id 字段，配合用户池上下文过滤器实现多用户池数据隔离。</p>
 */
@MappedSuperclass
public interface BaseUserPoolEntity {
    /** 所属用户池 ID */
    @Column(name = "tenant_id")
    UUID userPoolId();
}
