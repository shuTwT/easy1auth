package com.easy1auth.directory.model;

import com.easy1auth.persistence.model.BaseEntity;
import com.easy1auth.persistence.model.BaseTenantEntity;
import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.*;

/**
 * pool_user 实体（对应 pool_user 表）。
 *
 * <p>第三方接入的用户体系，按租户隔离，供第三方授权登录使用。
 * 此类用户不在系统后台登录，Token 也不得与管理端（admin_user）混用。
 * 包含登录凭证、验证状态、组织归属（部门/岗位）、企业身份源映射与自定义属性。</p>
 */
@Entity
@Table(name = "pool_user")
public interface PoolUserEntity extends BaseEntity, BaseTenantEntity {
    /** 登录用户名（租户内唯一，用于授权登录） */
    String username();

    /** 邮箱（可为 null，登录时可作备选标识） */
    @Nullable String email();

    /** 手机号（可为 null） */
    @Nullable String phone();

    /** 密码哈希（BCrypt 加密存储；可为 null，表示无密码用户） */
    @Column(name = "password_hash")
    @Nullable String passwordHash();

    /** 用户姓名/显示名 */
    String name();

    /** 头像地址（可为 null） */
    @Nullable String avatar();

    /** 用户状态：active（正常）/ disabled（禁用）/ locked（锁定） */
    String status();

    /** 邮箱是否已验证 */
    @Column(name = "email_verified")
    boolean emailVerified();

    /** 手机号是否已验证 */
    @Column(name = "phone_verified")
    boolean phoneVerified();

    /** 所属部门（可为 null） */
    @Nullable String department();

    /** 岗位名称（可为 null，与岗位表通过名称关联） */
    @Nullable String position();

    /** 企业身份源 ID（非空表示由企业身份源托管，本系统不可修改） */
    @Column(name = "enterprise_identity_source_id")
    @Nullable UUID enterpriseIdentitySourceId();

    /** 企业身份源中的外部 ID（用于同步映射） */
    @Column(name = "enterprise_identity_external_id")
    @Nullable String enterpriseIdentityExternalId();

    /** 自定义扩展属性（键值对，序列化 JSON 存储，可为 null） */
    @Serialized
    @Nullable Map<String, Object> customAttributes();

    /** 最近一次登录时间（可为 null，表示从未登录） */
    @Column(name = "last_login_at")
    @Nullable Instant lastLoginAt();

    /** 创建时间 */
    @Column(name = "created_at")
    Instant createdAt();

    /** 最后更新时间 */
    @Column(name = "updated_at")
    Instant updatedAt();
}
