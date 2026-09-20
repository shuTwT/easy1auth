package com.easy1auth.system.model;

import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.Table;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * 系统配置实体（对应 sys_config 表）。
 *
 * <p>保存不隶属于任何租户的全局配置。配置值统一以字符串持久化，类型转换由
 * system 模块的配置服务负责，避免业务模块直接依赖表结构。</p>
 */
@Entity
@Table(name = "sys_config")
public interface SystemConfigEntity {
    /** 稳定且唯一的配置键。 */
    @Id
    @Column(name = "config_key")
    String key();

    /** 配置值。 */
    @Column(name = "config_value")
    String value();

    /** 配置项说明。 */
    @Nullable
    String description();

    /** 创建时间。 */
    @Column(name = "created_at")
    Instant createdAt();

    /** 最后更新时间。 */
    @Column(name = "updated_at")
    Instant updatedAt();
}
