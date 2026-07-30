package com.easy1auth.audit;

import com.easy1auth.audit.model.*;
import com.easy1auth.foundation.error.DomainException;
import com.easy1auth.foundation.id.UuidV7;
import com.easy1auth.security.SecurityDataCipher;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Service
public class DeliveryService {
    private static final WebhookSubscriptionEntityTable HOOK = WebhookSubscriptionEntityTable.$;
    private static final DeliveryOutboxEntityTable OUT = DeliveryOutboxEntityTable.$;
    private final JSqlClient sql;
    private final SecurityDataCipher cipher;
    private final SecureRandom random = new SecureRandom();

    public DeliveryService(JSqlClient sql, SecurityDataCipher cipher) {
        this.sql = sql;
        this.cipher = cipher;
    }

    @Transactional
    public SubscriptionView create(UUID tenant, SubscriptionInput in) {
        validate(in);
        UUID id = UuidV7.randomUuid();
        String secret = token(32);
        Instant now = Instant.now();
        var e = WebhookSubscriptionEntityDraft.$.produce(d -> d.setId(id).setTenantId(tenant).setName(in.name().strip()).setUrl(in.url()).setEvents(in.events().stream().distinct().toList()).setSecretHash(hash(secret)).setEncryptedSecret(cipher.encrypt("webhook:" + tenant + ":" + id, secret)).setStatus("active").setMaxRetries(in.maxRetries() == null ? 5 : in.maxRetries()).setCreatedAt(now).setUpdatedAt(now));
        sql.saveCommand(e).setMode(SaveMode.INSERT_ONLY).execute();
        return view(e, secret);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionView> list(UUID tenant) {
        return sql.createQuery(HOOK).where(HOOK.tenantId().eq(tenant)).orderBy(HOOK.createdAt().desc()).select(HOOK).execute().stream().map(e -> view(e, null)).toList();
    }

    @Transactional
    public SubscriptionView update(UUID tenant, UUID id, SubscriptionInput in) {
        var old = entity(tenant, id);
        validate(in);
        sql.createUpdate(HOOK).set(HOOK.name(), in.name()).set(HOOK.url(), in.url()).set(HOOK.events(), in.events()).set(HOOK.maxRetries(), in.maxRetries() == null ? old.maxRetries() : in.maxRetries()).set(HOOK.status(), in.status() == null ? old.status() : status(in.status())).set(HOOK.updatedAt(), Instant.now()).where(HOOK.tenantId().eq(tenant), HOOK.id().eq(id)).execute();
        return view(entity(tenant, id), null);
    }

    @Transactional
    public SubscriptionView rotate(UUID tenant, UUID id) {
        entity(tenant, id);
        String secret = token(32);
        sql.createUpdate(HOOK).set(HOOK.secretHash(), hash(secret)).set(HOOK.encryptedSecret(), cipher.encrypt("webhook:" + tenant + ":" + id, secret)).set(HOOK.updatedAt(), Instant.now()).where(HOOK.id().eq(id), HOOK.tenantId().eq(tenant)).execute();
        return view(entity(tenant, id), secret);
    }

    @Transactional
    public void delete(UUID tenant, UUID id) {
        if (sql.createDelete(HOOK).where(HOOK.id().eq(id), HOOK.tenantId().eq(tenant)).execute() != 1) throw missing();
    }

    @Transactional
    public void enqueueEvent(UUID tenant, String event, Map<String, Object> payload, String key) {
        for (var hook : sql.createQuery(HOOK).where(HOOK.tenantId().eq(tenant), HOOK.status().eq("active")).select(HOOK).execute())
            if (hook.events().contains(event) || hook.events().contains("*"))
                enqueue(tenant, "webhook", hook.url(), event, payload, hook.id(), key + ":" + hook.id(), hook.maxRetries());
    }

    @Transactional
    public void enqueueEmail(UUID tenant, String to, String subject, String body, String key) {
        enqueue(tenant, "email", to, "email", Map.of("subject", subject, "body", body), null, key, 5);
    }

    @Transactional
    public List<DeliveryOutboxEntity> claim(int limit) {
        Instant now = Instant.now();
        var rows = sql.createQuery(OUT).where(Predicate.or(OUT.status().eq("pending"), Predicate.and(OUT.status().eq("processing"), OUT.leaseUntil().lt(now))), OUT.availableAt().le(now)).orderBy(OUT.availableAt()).select(OUT).limit(Math.min(50, Math.max(1, limit))).forUpdate().execute();
        for (var row : rows)
            sql.createUpdate(OUT).set(OUT.status(), "processing").set(OUT.leaseUntil(), now.plusSeconds(60)).where(OUT.id().eq(row.id())).execute();
        return rows;
    }

    @Transactional
    public void sent(UUID id) {
        sql.createUpdate(OUT).set(OUT.status(), "sent").set(OUT.sentAt(), Instant.now()).set(OUT.leaseUntil(), (Instant) null).where(OUT.id().eq(id)).execute();
    }

    @Transactional
    public void failed(UUID id, String error) {
        var row = sql.createQuery(OUT).where(OUT.id().eq(id)).select(OUT).forUpdate().fetchOne();
        int n = row.attempts() + 1;
        boolean dead = n >= row.maxAttempts();
        long delay = Math.min(3600L, 5L * (1L << Math.min(10, n)));
        sql.createUpdate(OUT).set(OUT.status(), dead ? "dead" : "pending").set(OUT.attempts(), n).set(OUT.availableAt(), Instant.now().plusSeconds(delay + random.nextInt(5))).set(OUT.leaseUntil(), (Instant) null).set(OUT.lastError(), trim(error, 1000)).where(OUT.id().eq(id)).execute();
    }

    @Transactional(readOnly = true)
    public String webhookSecret(DeliveryOutboxEntity row) {
        var hook = entity(row.tenantId(), row.subscriptionId());
        return cipher.decrypt("webhook:" + hook.tenantId() + ":" + hook.id(), hook.encryptedSecret());
    }

    private void enqueue(UUID tenant, String channel, String dest, String event, Map<String, Object> payload, UUID subscription, String key, int retries) {
        Instant now = Instant.now();
        var e = DeliveryOutboxEntityDraft.$.produce(d -> d.setId(UuidV7.randomUuid()).setTenantId(tenant).setChannel(channel).setDestination(dest).setEventType(event).setPayload(payload).setSubscriptionId(subscription).setIdempotencyKey(key).setStatus("pending").setAttempts(0).setMaxAttempts(retries).setAvailableAt(now).setLeaseUntil(null).setLastError(null).setCreatedAt(now).setSentAt(null));
        sql.saveCommand(e).setMode(SaveMode.INSERT_IF_ABSENT).execute();
    }

    private WebhookSubscriptionEntity entity(UUID tenant, UUID id) {
        return sql.createQuery(HOOK).where(HOOK.tenantId().eq(tenant), HOOK.id().eq(id)).select(HOOK).fetchOptional().orElseThrow(this::missing);
    }

    private DomainException missing() {
        return new DomainException("WEBHOOK_NOT_FOUND", "Webhook 不存在", 404);
    }

    private static void validate(SubscriptionInput in) {
        if (in == null || in.name() == null || in.name().isBlank() || in.events() == null || in.events().isEmpty())
            throw new DomainException("WEBHOOK_INVALID", "Webhook 参数不完整", 400);
        try {
            URI u = URI.create(in.url());
            if (!"https".equals(u.getScheme()) || u.getHost() == null) throw new IllegalArgumentException();
            for (var a : java.net.InetAddress.getAllByName(u.getHost()))
                if (a.isAnyLocalAddress() || a.isLoopbackAddress() || a.isLinkLocalAddress() || a.isSiteLocalAddress())
                    throw new IllegalArgumentException();
        } catch (Exception ex) {
            throw new DomainException("WEBHOOK_URL_FORBIDDEN", "Webhook 必须使用可公开访问的 HTTPS 地址", 400);
        }
        if (in.maxRetries() != null && (in.maxRetries() < 0 || in.maxRetries() > 20))
            throw new DomainException("WEBHOOK_RETRY_INVALID", "Webhook 重试次数无效", 400);
    }

    private String token(int n) {
        byte[] b = new byte[n];
        random.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private static String hash(String v) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(v.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String status(String v) {
        if (!Set.of("active", "disabled").contains(v))
            throw new DomainException("WEBHOOK_STATUS_INVALID", "Webhook 状态无效", 400);
        return v;
    }

    private static String trim(String v, int n) {
        return v == null ? null : v.substring(0, Math.min(n, v.length()));
    }

    private static SubscriptionView view(WebhookSubscriptionEntity e, String secret) {
        return new SubscriptionView(e.id(), e.tenantId(), e.name(), e.url(), e.events(), secret, e.status(), e.maxRetries(), e.createdAt(), e.updatedAt());
    }

    public record SubscriptionInput(String name, String url, List<String> events, Integer maxRetries, String status) {
    }

    public record SubscriptionView(UUID id, UUID tenantId, String name, String url, List<String> events, String secret,
                                   String status, int maxRetries, Instant createdAt, Instant updatedAt) {
    }
}
