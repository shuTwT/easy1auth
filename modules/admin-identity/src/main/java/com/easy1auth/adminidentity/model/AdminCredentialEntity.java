package com.easy1auth.adminidentity.model;

import org.babyfish.jimmer.sql.*;

import java.time.Instant;
import java.util.UUID;

/**
 * 管理账号凭证实体（对应 admin_credential 表）。
 *
 * <p>保存管理账号（admin_user）的登录凭证信息：密码哈希、最近修改时间等。
 * 一个账号对应一条凭证记录，账号 ID 即主键。</p>
 */
@Entity
@Table(name = "admin_credential")
public interface AdminCredentialEntity {
    /** 所属管理账号 ID（与 admin_account 一一对应） */
    @Id
    @Column(name = "account_id")
    UUID accountId();

    /** 密码哈希（由 BCrypt 等加密算法生成，不存明文） */
    @Column(name = "password_hash")
    String passwordHash();

    /** 密码最近一次修改时间 */
    @Column(name = "password_changed_at")
    Instant passwordChangedAt();

    /** 创建时间 */
    @Column(name = "created_at")
    Instant createdAt();

    /** 最后更新时间 */
    @Column(name = "updated_at")
    Instant updatedAt();
}
