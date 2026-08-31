package com.easy1auth.connection.model;

import com.easy1auth.common.persistence.model.BaseEntity;
import com.easy1auth.common.persistence.model.BaseTenantEntity;
import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 社会化身份绑定：pool_user 与外部社交账号（source_type + subject）的关联。
 *
 * <p>一个 pool_user 可绑定多个社交账号；同一外部账号在租户内的某身份源下
 * 只能绑定一个用户，重复绑定会触发冲突校验。</p>
 */
@Entity
@Table(name = "social_identity_binding")
public interface SocialIdentityBindingEntity extends BaseEntity, BaseTenantEntity {
    /** 身份源 ID（social_identity_source） */
    @Column(name = "source_id")
    UUID sourceId();

    /** 绑定的终端用户 ID（pool_user） */
    @Column(name = "pool_user_id")
    UUID poolUserId();

    /** 身份源类型，冗余存储便于跨 source 查询。 */
    @Column(name = "source_type")
    String sourceType();

    /** 外部社交账号唯一标识（openid / id 等）。 */
    String subject();

    /** 绑定时刻保存的第三方用户信息快照（JSON 序列化存储） */
    @Serialized
    Map<String, Object> claims();

    /** 创建时间 */
    @Column(name = "created_at")
    Instant createdAt();

    /** 最近一次通过该身份登录的时间（可为 null） */
    @Nullable
    @Column(name = "last_login_at")
    Instant lastLoginAt();
}
