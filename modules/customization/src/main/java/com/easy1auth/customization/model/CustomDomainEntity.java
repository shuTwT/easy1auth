package com.easy1auth.customization.model;

import com.easy1auth.common.persistence.model.BaseEntity;
import com.easy1auth.common.persistence.model.BaseTenantEntity;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * 自定义域名实体（对应 custom_domain 表）。
 *
 * <p>记录认证页自定义域名的所有权验证信息。租户添加域名后需通过
 * DNS 或文件方式完成所有权验证，验证通过前状态保持 pending。</p>
 */
@Entity
@Table(name = "custom_domain")
public interface CustomDomainEntity extends BaseEntity, BaseTenantEntity {

    /** 自定义域名（已做 IDN 规范化，如 auth.example.com） */
    String domain();

    /** 域名验证状态：pending（待验证）/ verified（已验证） */
    String status();

    /** 所有权验证方式：dns（DNS 记录）/ file（上传验证文件） */
    @Column(name = "verification_method")
    String verificationMethod();

    /** 用于所有权验证的随机令牌，需回填到 DNS 记录或验证文件中 */
    @Column(name = "verification_token")
    String verificationToken();

    /** 验证通过时间，未验证时为 null */
    @Nullable
    @Column(name = "verified_at")
    Instant verifiedAt();

    /** 创建时间 */
    @Column(name = "created_at")
    Instant createdAt();

    /** 最后更新时间 */
    @Column(name = "updated_at")
    Instant updatedAt();
}
