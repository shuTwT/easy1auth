package com.easy1auth.social.model;

import com.easy1auth.persistence.model.BaseEntity;
import com.easy1auth.persistence.model.BaseTenantEntity;
import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * 社会化身份源：管理员配置的预定义社交登录厂商（微信/Github/Gitee/飞书等），
 * 端点写死在适配器内，不存储 issuer。管理员只需填写 name/type/clientId/clientSecret。
 */
@Entity
@Table(name = "social_identity_source")
public interface SocialIdentitySourceEntity extends BaseEntity, BaseTenantEntity {
    String name();

    /** 厂商类型，如 wechat_qr / wechat_mp / github / gitee / feishu_web。 */
    @Column(name = "type")
    String type();

    /** 厂商子模式，当前仅微信区分 qr/mp，其余为 null。 */
    @Nullable
    @Column(name = "mode")
    String mode();

    @Column(name = "client_id")
    String clientId();

    @Column(name = "encrypted_client_secret")
    String encryptedClientSecret();

    @Column(name = "jit_provisioning")
    boolean jitProvisioning();

    String status();

    @Column(name = "created_at")
    Instant createdAt();

    @Column(name = "updated_at")
    Instant updatedAt();
}
