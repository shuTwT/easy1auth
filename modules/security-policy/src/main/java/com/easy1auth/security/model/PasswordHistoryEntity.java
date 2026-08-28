package com.easy1auth.security.model;

import org.babyfish.jimmer.sql.*;

import java.time.Instant;
import java.util.UUID;

/**
 * 密码历史实体（对应 password_history 表）。
 *
 * <p>记录主体最近使用过的密码哈希，用于修改密码时禁止与历史密码重复
 * （防重用）。保留条数由安全策略的 historyCount 控制。</p>
 */
@Entity
@Table(name = "password_history")
public interface PasswordHistoryEntity {
    /** 历史记录 ID */
    @Id
    UUID id();

    /** 主体类型（区分 admin_user / pool_user 等） */
    @Column(name = "subject_type")
    String subjectType();

    /** 主体 ID */
    @Column(name = "subject_id")
    UUID subjectId();

    /** 历史密码的加密哈希（BCrypt 等，明文不存储） */
    @Column(name = "password_hash")
    String passwordHash();

    /** 创建时间（用于按新旧裁剪记录） */
    @Column(name = "created_at")
    Instant createdAt();
}
