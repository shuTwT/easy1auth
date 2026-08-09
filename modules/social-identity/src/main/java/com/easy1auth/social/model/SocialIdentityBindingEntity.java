package com.easy1auth.social.model;

import com.easy1auth.persistence.model.BaseEntity;
import com.easy1auth.persistence.model.BaseTenantEntity;
import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * 社会化身份绑定：pool_user 与外部社交账号（source_type + subject）的关联。
 */
@Entity
@Table(name = "social_identity_binding")
public interface SocialIdentityBindingEntity extends BaseEntity, BaseTenantEntity {
    @Column(name = "source_id")
    UUID sourceId();

    @Column(name = "pool_user_id")
    UUID poolUserId();

    /** 身份源类型，冗余存储便于跨 source 查询。 */
    @Column(name = "source_type")
    String sourceType();

    /** 外部社交账号唯一标识（openid / id 等）。 */
    String subject();

    @Serialized
    Map<String, Object> claims();

    @Column(name = "created_at")
    Instant createdAt();

    @Nullable
    @Column(name = "last_login_at")
    Instant lastLoginAt();
}
