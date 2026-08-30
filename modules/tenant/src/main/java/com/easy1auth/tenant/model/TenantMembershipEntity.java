package com.easy1auth.tenant.model;

import com.easy1auth.common.persistence.model.BaseEntity;
import com.easy1auth.common.persistence.model.BaseTenantEntity;

import org.babyfish.jimmer.sql.*;

import java.time.Instant;
import java.util.UUID;

/**
 * 租户成员实体（对应 tenant_membership 表）。
 *
 * <p>记录管理账号（admin_user）与租户的从属关系。一个管理账号可以拥有多个租户，
 * 在每个租户中承担一个角色（tenant_admin / common / super_admin），并可被停用。</p>
 */
@Entity
@Table(name = "tenant_membership")
public interface TenantMembershipEntity extends BaseEntity, BaseTenantEntity {
    /** 管理账号 ID */
    @Column(name = "account_id")
    UUID accountId();

    /** 账号在该租户内的角色：tenant_admin（租户管理员）/ common（普通成员）/ super_admin（平台超管） */
    @Column(name = "membership_role")
    String membershipRole();

    /** 成员关系状态：active（正常）/ suspended（停用） */
    String status();

    /** 创建时间 */
    @Column(name = "created_at")
    Instant createdAt();

    /** 最后更新时间 */
    @Column(name = "updated_at")
    Instant updatedAt();
}
