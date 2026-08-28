package com.easy1auth.directory.model;

import com.easy1auth.persistence.model.BaseEntity;
import com.easy1auth.persistence.model.BaseTenantEntity;
import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * 用户组实体（对应 user_group 表）。
 *
 * <p>描述 pool_user 的组织架构中的用户组，按租户隔离。用户组支持多种类型
 * （team / department / project / organization），可通过 {@code parentId}
 * 组成树形层级；由企业身份源（enterpriseIdentitySourceId）托管的组
 * 不可在本系统内修改。</p>
 */
@Entity
@Table(name = "user_group")
public interface UserGroupEntity extends BaseEntity, BaseTenantEntity {
    /** 用户组名称（中文展示名） */
    String name();

    /** 用户组描述（可为 null） */
    @Nullable String description();

    /** 用户组类型：team（团队）/ department（部门）/ project（项目）/ organization（组织） */
    String type();

    /** 父组 ID（可为 null，表示顶级组） */
    @Column(name = "parent_id")
    @Nullable UUID parentId();

    /** 企业身份源 ID（非空表示由企业身份源托管，本系统不可修改） */
    @Column(name = "enterprise_identity_source_id")
    @Nullable UUID enterpriseIdentitySourceId();

    /** 企业身份源中的外部 ID（用于同步映射） */
    @Column(name = "enterprise_identity_external_id")
    @Nullable String enterpriseIdentityExternalId();

    /** 创建时间 */
    @Column(name = "created_at")
    Instant createdAt();

    /** 最后更新时间 */
    @Column(name = "updated_at")
    Instant updatedAt();
}
