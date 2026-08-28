package com.easy1auth.security.model;

import com.easy1auth.infrastructure.persistence.model.BaseEntity;
import com.easy1auth.infrastructure.persistence.model.BaseTenantEntity;
import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

/**
 * 终端用户设备实体（对应 pool_user_device 表）。
 *
 * <p>记录 pool_user 的登录来源设备，以「用户代理 + IP」指纹的 SHA-256 哈希
 * 识别设备，用于多设备管理、撤销信任与异常登录排查。</p>
 */
@Entity
@Table(name = "pool_user_device")
public interface PoolUserDeviceEntity extends BaseEntity, BaseTenantEntity {
    /** 终端用户 ID（pool_user） */
    @Column(name = "user_id")
    UUID userId();

    /** 设备指纹的 SHA-256 哈希（由用户代理与 IP 计算，明文不存储） */
    @Column(name = "device_token_hash")
    String deviceTokenHash();

    /** 用户代理（可为 null） */
    @Nullable
    @Column(name = "user_agent")
    String userAgent();

    /** 最近登录 IP（可为 null） */
    @Nullable
    @Column(name = "ip_address")
    String ipAddress();

    /** 首次发现该设备的时间 */
    @Column(name = "first_seen_at")
    Instant firstSeenAt();

    /** 最后活跃时间 */
    @Column(name = "last_seen_at")
    Instant lastSeenAt();

    /** 撤销时间，非空表示该设备已被注销、不再受信任 */
    @Nullable
    @Column(name = "revoked_at")
    Instant revokedAt();
}
