package com.easy1auth.common.persistence.model;

import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.MappedSuperclass;

import java.util.UUID;

/**
 * 实体主键基类：所有 jimmer 实体的公共主键定义。
 *
 * <p>统一使用 UUID 作为主键，列名为 id。</p>
 */
@MappedSuperclass
public interface BaseEntity {
    /** 实体主键 ID */
    @Id
    UUID id();
}
