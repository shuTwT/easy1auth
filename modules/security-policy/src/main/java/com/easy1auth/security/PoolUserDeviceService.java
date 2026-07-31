package com.easy1auth.security;

import com.easy1auth.foundation.id.UuidV7;
import com.easy1auth.security.model.*;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

@Service
public class PoolUserDeviceService {
    private static final PoolUserDeviceEntityTable DEVICE = PoolUserDeviceEntityTable.$;
    private final JSqlClient sql;

    public PoolUserDeviceService(JSqlClient sql) {
        this.sql = sql;
    }

    @Transactional
    public void seen(UUID tenant, UUID user, String userAgent, String ip) {
        String hash = hash(Objects.toString(userAgent, "unknown"));
        var old = sql.createQuery(DEVICE).where(DEVICE.tenantId().eq(tenant), DEVICE.userId().eq(user), DEVICE.deviceTokenHash().eq(hash)).select(DEVICE).fetchOneOrNull();
        if (old == null) {
            Instant now = Instant.now();
            var e = PoolUserDeviceEntityDraft.$.produce(d -> d.setId(UuidV7.randomUuid()).setTenantId(tenant).setUserId(user).setDeviceTokenHash(hash).setUserAgent(trim(userAgent, 512)).setIpAddress(trim(ip, 64)).setFirstSeenAt(now).setLastSeenAt(now).setRevokedAt(null));
            sql.saveCommand(e).setMode(SaveMode.INSERT_IF_ABSENT).execute();
        } else
            sql.createUpdate(DEVICE).set(DEVICE.lastSeenAt(), Instant.now()).set(DEVICE.ipAddress(), trim(ip, 64)).where(DEVICE.id().eq(old.id())).execute();
    }

    @Transactional(readOnly = true)
    public List<PoolUserDeviceEntity> list(UUID tenant, UUID user) {
        return sql.createQuery(DEVICE).where(DEVICE.tenantId().eq(tenant), DEVICE.userId().eq(user)).orderBy(DEVICE.lastSeenAt().desc()).select(DEVICE).execute();
    }

    @Transactional
    public void revoke(UUID tenant, UUID user, UUID id) {
        sql.createUpdate(DEVICE).set(DEVICE.revokedAt(), Instant.now()).where(DEVICE.tenantId().eq(tenant), DEVICE.userId().eq(user), DEVICE.id().eq(id)).execute();
    }

    private static String hash(String v) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String trim(String v, int n) {
        return v == null ? null : v.substring(0, Math.min(n, v.length()));
    }
}
