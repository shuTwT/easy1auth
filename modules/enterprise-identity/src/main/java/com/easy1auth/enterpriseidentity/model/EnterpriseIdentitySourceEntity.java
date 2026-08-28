package com.easy1auth.enterpriseidentity.model;

import com.easy1auth.infrastructure.persistence.model.BaseEntity;
import com.easy1auth.infrastructure.persistence.model.BaseTenantEntity;
import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * 企业身份源实体（对应 enterprise_identity_source 表）。
 *
 * <p>描述一个租户配置的飞书企业身份源，包含接入凭证（app_id /
 * app_secret、事件订阅的验证令牌与解密密钥）。敏感凭证加密后存储，
 * 用于将飞书通讯录同步到本地用户体系。</p>
 */
@Entity
@Table(name = "enterprise_identity_source")
public interface EnterpriseIdentitySourceEntity extends BaseEntity, BaseTenantEntity {
    /** 身份源名称 */
    String name();
    /** 身份源类型（当前仅支持 feishu） */
    String provider();
    /** 飞书开放平台应用 App ID */
    @Column(name = "app_id") String appId();
    /** 加密后的 App Secret */
    @Column(name = "encrypted_app_secret") String encryptedAppSecret();
    /** 加密后的事件验证令牌（用于回调签名校验） */
    @Column(name = "encrypted_verification_token") String encryptedVerificationToken();
    /** 加密后的事件解密密钥 */
    @Column(name = "encrypted_encrypt_key") String encryptedEncryptKey();
    /** 身份源状态：active（启用）/ disabled（停用） */
    String status();
    /** 最近一次同步时间 */
    @Nullable @Column(name = "last_sync_at") Instant lastSyncAt();
    /** 最近一次同步结果：succeeded / partial / failed */
    @Nullable @Column(name = "last_sync_status") String lastSyncStatus();
    /** 最近一次同步的错误信息 */
    @Nullable @Column(name = "last_error") String lastError();
    /** 创建时间 */
    @Column(name = "created_at") Instant createdAt();
    /** 最后更新时间 */
    @Column(name = "updated_at") Instant updatedAt();
}
