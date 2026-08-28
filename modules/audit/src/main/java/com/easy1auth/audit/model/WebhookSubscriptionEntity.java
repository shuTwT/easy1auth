package com.easy1auth.audit.model;

import com.easy1auth.persistence.model.BaseEntity;
import com.easy1auth.persistence.model.BaseTenantEntity;

import org.babyfish.jimmer.sql.*;

import java.time.Instant;
import java.util.*;

/**
 * Webhook 订阅实体（对应 webhook_subscription 表）。
 *
 * <p>记录租户订阅的审计事件回调：订阅了哪些事件（含 "*" 表示全部事件）、
 * 回调地址与最大重试次数。密钥不落明文，仅保存 SHA-256 摘要与对称加密
 * 密文，用于投递时的签名校验与解密。</p>
 */
@Entity
@Table(name = "webhook_subscription")
public interface WebhookSubscriptionEntity extends BaseEntity, BaseTenantEntity {

    /** 订阅名称 */
    String name();

    /** 回调地址（须为可公开访问的 HTTPS） */
    String url();

    /** 订阅的审计事件类型列表（含 "*" 表示全部事件） */
    @Serialized
    List<String> events();

    /** 密钥的 SHA-256 摘要（用于签名校验） */
    @Column(name = "secret_hash")
    String secretHash();

    /** 密钥的对称加密密文（用于投递时解密） */
    @Column(name = "encrypted_secret")
    String encryptedSecret();

    /** 订阅状态：active（启用）/ disabled（停用） */
    String status();

    /** 最大重试次数（0-20） */
    @Column(name = "max_retries")
    int maxRetries();

    /** 创建时间 */
    @Column(name = "created_at")
    Instant createdAt();

    /** 最后更新时间 */
    @Column(name = "updated_at")
    Instant updatedAt();
}
