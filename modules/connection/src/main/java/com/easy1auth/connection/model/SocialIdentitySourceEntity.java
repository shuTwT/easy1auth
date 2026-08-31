package com.easy1auth.connection.model;

import com.easy1auth.common.persistence.model.BaseEntity;
import com.easy1auth.common.persistence.model.BaseTenantEntity;
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
    /** 身份源显示名称 */
    String name();

    /** 厂商类型，如 wechat_qr / wechat_mp / github / gitee / feishu_web。 */
    @Column(name = "type")
    String type();

    /** 厂商子模式，当前仅微信区分 qr/mp，其余为 null。 */
    @Nullable
    @Column(name = "mode")
    String mode();

    /** 厂商应用 client_id / appid */
    @Column(name = "client_id")
    String clientId();

    /** 厂商应用密钥的密文（AES-GCM，明文不落库） */
    @Column(name = "encrypted_client_secret")
    String encryptedClientSecret();

    /** 是否在回调时自动开通新用户（JIT provisioning） */
    @Column(name = "jit_provisioning")
    boolean jitProvisioning();

    /** 状态：active（启用）/ disabled（停用） */
    String status();

    /** 创建时间 */
    @Column(name = "created_at")
    Instant createdAt();

    /** 最后更新时间 */
    @Column(name = "updated_at")
    Instant updatedAt();
}
