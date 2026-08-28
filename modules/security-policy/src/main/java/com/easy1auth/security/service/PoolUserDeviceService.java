package com.easy1auth.security.service;

import com.easy1auth.infrastructure.foundation.id.UuidV7;
import com.easy1auth.security.model.*;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

/**
 * 终端用户设备管理服务：记录登录来源设备并支持查看与撤销。
 *
 * <p>以「用户代理 + IP」指纹（SHA-256 哈希）识别设备，记录首次/最后活跃时间；
 * 撤销后该设备不再受信任，用于多设备登录管理与异常登录排查。</p>
 */
@Service
public class PoolUserDeviceService {
    /** pool_user_device 表静态描述符 */
    private static final PoolUserDeviceEntityTable DEVICE = PoolUserDeviceEntityTable.$;
    /** jimmer SQL 客户端 */
    private final JSqlClient sql;

    public PoolUserDeviceService(JSqlClient sql) {
        this.sql = sql;
    }

    /** 登记一次设备活跃：新设备则创建记录，已存在则刷新最后活跃时间与 IP。 */
    @Transactional
    public void seen(UUID tenant, UUID user, String userAgent, String ip) {
        String hash = hash(Objects.toString(userAgent, "unknown"));
        var old = sql.createQuery(DEVICE).where(DEVICE.tenantId().eq(tenant), DEVICE.userId().eq(user), DEVICE.deviceTokenHash().eq(hash)).select(DEVICE).fetchOneOrNull();
        if (old == null) {
            Instant now = Instant.now();
            var e = PoolUserDeviceEntityDraft.$.produce(d -> d.setId(UuidV7.randomUuid()).setTenantId(tenant).setUserId(user).setDeviceTokenHash(hash).setUserAgent(trim(userAgent, 512)).setIpAddress(trim(ip, 64)).setFirstSeenAt(now).setLastSeenAt(now).setRevokedAt(null));
            sql.saveCommand(e).setMode(SaveMode.INSERT_IF_ABSENT).execute();
        } else {
            sql.createUpdate(DEVICE).set(DEVICE.lastSeenAt(), Instant.now()).set(DEVICE.ipAddress(), trim(ip, 64)).where(DEVICE.id().eq(old.id())).execute();
        }
    }

    /** 查询某用户名下的设备列表，按最后活跃时间倒序。 */
    @Transactional(readOnly = true)
    public List<PoolUserDeviceEntity> list(UUID tenant, UUID user) {
        return sql.createQuery(DEVICE).where(DEVICE.tenantId().eq(tenant), DEVICE.userId().eq(user)).orderBy(DEVICE.lastSeenAt().desc()).select(DEVICE).execute();
    }

    /** 撤销指定设备：置为已撤销时间，使其不再受信任。 */
    @Transactional
    public void revoke(UUID tenant, UUID user, UUID id) {
        sql.createUpdate(DEVICE).set(DEVICE.revokedAt(), Instant.now()).where(DEVICE.tenantId().eq(tenant), DEVICE.userId().eq(user), DEVICE.id().eq(id)).execute();
    }

    /** 对设备指纹做 SHA-256 十六进制哈希，避免明文存储用户代理。 */
    private static String hash(String v) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /** 截断字符串至指定最大长度（防止超长 UA/IP 入库）。 */
    private static String trim(String v, int n) {
        return v == null ? null : v.substring(0, Math.min(n, v.length()));
    }
}
