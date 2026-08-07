package com.easy1auth.enterpriseidentity;

import com.easy1auth.directory.PoolUserService;
import com.easy1auth.directory.model.*;
import com.easy1auth.enterpriseidentity.model.*;
import com.easy1auth.foundation.error.DomainException;
import com.easy1auth.foundation.id.UuidV7;
import com.easy1auth.foundation.web.PageData;
import com.easy1auth.security.SecurityDataCipher;
import com.easy1auth.tenant.TenantContextHolder;
import com.easy1auth.tenant.TenantUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

/**
 * Tenant-owned enterprise directory provisioning. The Feishu adapter intentionally does not provide SSO.
 */
@Service
public class EnterpriseIdentityService {
    private static final EnterpriseIdentitySourceEntityTable SOURCE = EnterpriseIdentitySourceEntityTable.$;
    private static final EnterpriseIdentitySyncTaskEntityTable TASK = EnterpriseIdentitySyncTaskEntityTable.$;
    private static final PoolUserEntityTable USER = PoolUserEntityTable.$;
    private static final UserGroupEntityTable GROUP = UserGroupEntityTable.$;
    private static final UserGroupAssignmentEntityTable MEMBERSHIP = UserGroupAssignmentEntityTable.$;
    private final JSqlClient sql;
    private final SecurityDataCipher cipher;
    private final ObjectMapper json;
    private final PoolUserService users;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

    public EnterpriseIdentityService(JSqlClient sql, SecurityDataCipher cipher, ObjectMapper json, PoolUserService users) {
        this.sql = sql;
        this.cipher = cipher;
        this.json = json;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public PageData<SourceView> list(int page, int pageSize, String search, String status) {
        UUID tenant = TenantContextHolder.requireTenantId();
        int p = Math.max(page, 1), s = Math.min(Math.max(pageSize, 1), 100);
        var q = sql.createQuery(SOURCE).where(SOURCE.tenantId().eq(tenant))
                .whereIf(search != null && !search.isBlank(), () -> SOURCE.name().ilike(search, LikeMode.ANYWHERE))
                .whereIf(status != null && !status.isBlank(), () -> SOURCE.status().eq(status))
                .orderBy(SOURCE.createdAt().desc()).select(SOURCE);
        return PageData.of(q.limit(s, (long) (p - 1) * s).execute().stream().map(EnterpriseIdentityService::view).toList(), p, s, q.fetchUnlimitedCount());
    }

    @Transactional(readOnly = true)
    public SourceView get(UUID id) {
        return view(source(TenantContextHolder.requireTenantId(), id));
    }

    @Transactional
    public SourceView create(Input input) {
        UUID tenant = TenantContextHolder.requireTenantId();
        validate(input, true);
        UUID id = UuidV7.randomUuid();
        Instant now = Instant.now();
        var row = EnterpriseIdentitySourceEntityDraft.$.produce(d -> d.setId(id).setTenantId(tenant).setName(input.name().strip()).setProvider("feishu")
                .setAppId(input.appId().strip()).setEncryptedAppSecret(encrypt(tenant, id, "app-secret", input.appSecret()))
                .setEncryptedVerificationToken(encrypt(tenant, id, "verification-token", input.verificationToken()))
                .setEncryptedEncryptKey(encrypt(tenant, id, "encrypt-key", input.encryptKey())).setStatus("active")
                .setLastSyncAt(null).setLastSyncStatus(null).setLastError(null).setCreatedAt(now).setUpdatedAt(now));
        sql.saveCommand(row).setMode(SaveMode.INSERT_ONLY).execute();
        return view(row);
    }

    @Transactional
    public SourceView update(UUID id, Input input) {
        UUID tenant = TenantContextHolder.requireTenantId();
        var old = source(tenant, id);
        validate(input, false);
        var update = sql.createUpdate(SOURCE).set(SOURCE.updatedAt(), Instant.now()).where(SOURCE.id().eq(id), SOURCE.tenantId().eq(tenant));
        if (input.name() != null) update.set(SOURCE.name(), input.name().strip());
        if (input.appId() != null) update.set(SOURCE.appId(), input.appId().strip());
        if (present(input.appSecret()))
            update.set(SOURCE.encryptedAppSecret(), encrypt(tenant, id, "app-secret", input.appSecret()));
        if (present(input.verificationToken()))
            update.set(SOURCE.encryptedVerificationToken(), encrypt(tenant, id, "verification-token", input.verificationToken()));
        if (present(input.encryptKey()))
            update.set(SOURCE.encryptedEncryptKey(), encrypt(tenant, id, "encrypt-key", input.encryptKey()));
        if (input.status() != null) update.set(SOURCE.status(), status(input.status()));
        update.execute();
        return get(id);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenant = TenantContextHolder.requireTenantId();
        source(tenant, id);
        // Imported data deliberately survives source deletion and becomes locally managed.
        sql.createUpdate(USER).set(USER.enterpriseIdentitySourceId(), (UUID) null).set(USER.enterpriseIdentityExternalId(), (String) null).where(USER.tenantId().eq(tenant), USER.enterpriseIdentitySourceId().eq(id)).execute();
        sql.createUpdate(GROUP).set(GROUP.enterpriseIdentitySourceId(), (UUID) null).set(GROUP.enterpriseIdentityExternalId(), (String) null).where(GROUP.tenantId().eq(tenant), GROUP.enterpriseIdentitySourceId().eq(id)).execute();
        sql.createDelete(SOURCE).where(SOURCE.id().eq(id), SOURCE.tenantId().eq(tenant)).execute();
    }

    @Transactional
    public TaskView sync(UUID id) {
        var source = source(TenantContextHolder.requireTenantId(), id);
        if (!"active".equals(source.status()))
            throw new DomainException(ErrorCodeConstants.ENTERPRISE_IDENTITY_SOURCE_DISABLED);
        return queue(source, "full", null, Map.of());
    }

    @Transactional(readOnly = true)
    public List<TaskView> tasks(UUID id) {
        UUID tenant = TenantContextHolder.requireTenantId();
        source(tenant, id);
        return sql.createQuery(TASK).where(TASK.tenantId().eq(tenant), TASK.sourceId().eq(id)).orderBy(TASK.createdAt().desc()).select(TASK).limit(30).execute().stream().map(EnterpriseIdentityService::taskView).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> stats() {
        UUID tenant = TenantContextHolder.requireTenantId();
        var rows = sql.createQuery(SOURCE).where(SOURCE.tenantId().eq(tenant)).select(SOURCE.status()).execute();
        long active = rows.stream().filter("active"::equals).count();
        return Map.of("totalSources", (long) rows.size(), "activeSources", active, "inactiveSources", rows.size() - active);
    }

    /**
     * Public callback path resolves a source without trusting a request tenant header.
     */
    @Transactional
    public Map<String, Object> acceptFeishuEvent(UUID sourceId, Map<String, Object> envelope) {
        EnterpriseIdentitySourceEntity source = ignored(() -> sql.findById(EnterpriseIdentitySourceEntity.class, sourceId));
        if (source == null || !"feishu".equals(source.provider()))
            throw new DomainException(ErrorCodeConstants.ENTERPRISE_IDENTITY_SOURCE_NOT_FOUND);
        Map<String, Object> event = decrypted(source, envelope);
        String token = string(event.get("token"));
        if (!MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8), decrypt(source, "verification-token").getBytes(StandardCharsets.UTF_8)))
            throw new DomainException(ErrorCodeConstants.FEISHU_EVENT_UNAUTHORIZED);
        if ("url_verification".equals(string(event.get("type"))))
            return Map.of("challenge", string(event.get("challenge")));
        if (!"active".equals(source.status())) return Map.of();
        Map<String, Object> header = map(event.get("header"));
        String eventId = string(header.get("event_id"));
        if (eventId.isBlank()) throw new DomainException(ErrorCodeConstants.FEISHU_EVENT_INVALID);
        Map<String, Object> queued = new LinkedHashMap<>(event);
        queued.remove("token");
        try {
            TenantUtils.execute(source.tenantId(), (Runnable) () -> queue(source, "event", eventId, queued));
        } catch (RuntimeException ex) {
            if (!isDuplicate(ex)) throw ex;
        }
        return Map.of();
    }

    @Transactional
    public List<TaskView> claim(int limit) {
        List<EnterpriseIdentitySyncTaskEntity> rows = sql.createQuery(TASK).where(TASK.status().eq("pending")).orderBy(TASK.createdAt().asc()).select(TASK).limit(Math.max(1, Math.min(limit, 20))).execute();
        List<TaskView> claimed = new ArrayList<>();
        for (var row : rows)
            if (sql.createUpdate(TASK).set(TASK.status(), "processing").set(TASK.startedAt(), Instant.now()).where(TASK.id().eq(row.id()), TASK.status().eq("pending")).execute() == 1)
                claimed.add(taskView(row));
        return claimed;
    }

    public void process(UUID taskId) {
        EnterpriseIdentitySyncTaskEntity task = ignored(() -> sql.findById(EnterpriseIdentitySyncTaskEntity.class, taskId));
        if (task == null || !"processing".equals(task.status())) return;
        TenantUtils.execute(task.tenantId(), () -> processInTenant(task));
    }

    @Transactional
    void processInTenant(EnterpriseIdentitySyncTaskEntity task) {
        var source = source(task.tenantId(), task.sourceId());
        Map<String, Object> summary = new LinkedHashMap<>();
        try {
            if ("full".equals(task.type())) full(source, summary);
            else event(source, task.payload(), summary);
            boolean partial = summary.containsKey("skippedMissingPhone") || summary.containsKey("skippedEmailConflict") || summary.containsKey("skippedPhoneConflict");
            finish(task.id(), summary, partial ? "partial" : "succeeded", null);
        } catch (Exception ex) {
            finish(task.id(), summary, "failed", trim(ex.getMessage()));
        }
    }

    private void full(EnterpriseIdentitySourceEntity source, Map<String, Object> summary) throws Exception {
        String token = token(source);
        Set<String> seenUsers = new HashSet<>();
        List<String> roots = scopeDepartments(token);
        if (roots.isEmpty()) roots = List.of("0");
        for (String root : roots) syncDepartmentTree(source, token, root, null, seenUsers, summary, new HashSet<>());
        summary.putIfAbsent("users", seenUsers.size());
    }

    private void syncDepartmentTree(EnterpriseIdentitySourceEntity source, String token, String externalId, UUID parent, Set<String> seenUsers, Map<String, Object> summary, Set<String> traversed) throws Exception {
        if (!traversed.add(externalId)) return;
        if (!"0".equals(externalId)) {
            Map<String, Object> department = data(get(token, "/open-apis/contact/v3/departments/" + enc(externalId) + "?department_id_type=open_department_id"));
            parent = upsertGroup(source, externalId, string(department.get("name")), parent);
        }
        for (Map<String, Object> user : paged(token, "/open-apis/contact/v3/users/find_by_department?department_id_type=open_department_id&user_id_type=open_id&department_id=" + enc(externalId) + "&page_size=50")) {
            String externalUser = string(user.get("open_id"));
            if (!externalUser.isBlank()) {
                if (seenUsers.add(externalUser)) upsertUser(source, externalUser, user, summary);
                syncMembership(source, externalUser, ids(user, "department_ids"));
            }
        }
        for (Map<String, Object> child : paged(token, "/open-apis/contact/v3/departments/" + enc(externalId) + "/children?department_id_type=open_department_id&page_size=50"))
            syncDepartmentTree(source, token, string(child.get("open_department_id")), parent, seenUsers, summary, traversed);
    }

    private void event(EnterpriseIdentitySourceEntity source, Map<String, Object> payload, Map<String, Object> summary) throws Exception {
        Map<String, Object> header = map(payload.get("header")), body = map(payload.get("event")), object = map(body.get("object"));
        String type = string(header.get("event_type"));
        if (type.startsWith("contact.user.")) {
            String id = string(object.get("open_id"));
            if (type.endsWith("deleted_v3")) {
                disableUser(source, id);
                summary.put("disabled", 1);
            } else if (!id.isBlank()) {
                Map<String, Object> user = data(get(token(source), "/open-apis/contact/v3/users/" + enc(id) + "?user_id_type=open_id"));
                upsertUser(source, id, user, summary);
                syncMembership(source, id, ids(user, "department_ids"));
            }
        } else if (type.startsWith("contact.department.") && !type.endsWith("deleted_v3")) {
            String id = string(object.get("open_department_id"));
            if (!id.isBlank()) {
                Map<String, Object> d = data(get(token(source), "/open-apis/contact/v3/departments/" + enc(id) + "?department_id_type=open_department_id"));
                upsertGroup(source, id, string(d.get("name")), sourceGroupId(source, string(d.get("parent_department_id"))));
                summary.put("departments", 1);
            }
        }
    }

    private UUID upsertGroup(EnterpriseIdentitySourceEntity source, String externalId, String name, UUID parent) {
        UUID tenant = source.tenantId();
        var old = sql.createQuery(GROUP).where(GROUP.tenantId().eq(tenant), GROUP.enterpriseIdentitySourceId().eq(source.id()), GROUP.enterpriseIdentityExternalId().eq(externalId)).select(GROUP).fetchOneOrNull();
        String safe = uniqueGroupName(tenant, old == null ? null : old.id(), parent, name);
        if (old == null) {
            UUID id = UuidV7.randomUuid();
            Instant now = Instant.now();
            sql.saveCommand(UserGroupEntityDraft.$.produce(d -> d.setId(id).setTenantId(tenant).setName(safe).setDescription("由飞书同步管理").setType("department").setParentId(parent).setEnterpriseIdentitySourceId(source.id()).setEnterpriseIdentityExternalId(externalId).setCreatedAt(now).setUpdatedAt(now))).setMode(SaveMode.INSERT_ONLY).execute();
            return id;
        }
        sql.createUpdate(GROUP).set(GROUP.name(), safe).set(GROUP.parentId(), parent).set(GROUP.updatedAt(), Instant.now()).where(GROUP.id().eq(old.id())).execute();
        return old.id();
    }

    private void upsertUser(EnterpriseIdentitySourceEntity source, String externalId, Map<String, Object> remote, Map<String, Object> summary) {
        UUID tenant = source.tenantId();
        String emailValue = string(remote.get("email")).strip().toLowerCase();
        String email = emailValue.isBlank() ? null : emailValue;
        String phone = string(remote.get("mobile")).strip();
        if (phone.isBlank()) {
            increment(summary, "skippedMissingPhone");
            return;
        }
        var old = sql.createQuery(USER).where(USER.tenantId().eq(tenant), USER.enterpriseIdentitySourceId().eq(source.id()), USER.enterpriseIdentityExternalId().eq(externalId)).select(USER).fetchOneOrNull();
        if (old == null && email != null && sql.createQuery(USER).where(USER.tenantId().eq(tenant), USER.email().eq(email)).select(USER.id()).exists()) {
            increment(summary, "skippedEmailConflict");
            return;
        }
        if (old == null && sql.createQuery(USER).where(USER.tenantId().eq(tenant), USER.phone().eq(phone)).select(USER.id()).exists()) {
            increment(summary, "skippedPhoneConflict");
            return;
        }
        String name = nonBlank(string(remote.get("name")), phone), avatar = string(map(remote.get("avatar")).get("avatar_240")), position = string(remote.get("job_title"));
        String department = "";
        List<String> deps = ids(remote, "department_ids");
        if (!deps.isEmpty()) {
            var g = sql.createQuery(GROUP).where(GROUP.enterpriseIdentitySourceId().eq(source.id()), GROUP.enterpriseIdentityExternalId().eq(deps.getFirst())).select(GROUP).fetchOneOrNull();
            if (g != null) department = g.name();
        }
        if (old == null) {
            String username = username(externalId);
            var created = users.create(tenant, new PoolUserService.Input(username, email, null, phone, name, avatar, "active", department, position, Map.of("enterpriseIdentity", "feishu")));
            sql.createUpdate(USER).set(USER.enterpriseIdentitySourceId(), source.id()).set(USER.enterpriseIdentityExternalId(), externalId).where(USER.id().eq(created.id())).execute();
            increment(summary, "createdUsers");
        } else {
            sql.createUpdate(USER).set(USER.email(), email).set(USER.name(), name).set(USER.phone(), phone).set(USER.avatar(), avatar).set(USER.department(), department).set(USER.position(), position).set(USER.status(), "active").set(USER.updatedAt(), Instant.now()).where(USER.id().eq(old.id())).execute();
            increment(summary, "updatedUsers");
        }
    }

    private void syncMembership(EnterpriseIdentitySourceEntity source, String externalUserId, List<String> departments) {
        var user = sql.createQuery(USER).where(USER.enterpriseIdentitySourceId().eq(source.id()), USER.enterpriseIdentityExternalId().eq(externalUserId)).select(USER).fetchOneOrNull();
        if (user == null) return;
        var sourceGroups = sql.createQuery(GROUP).where(GROUP.enterpriseIdentitySourceId().eq(source.id())).select(GROUP).execute();
        Set<UUID> ids = sourceGroups.stream().filter(group -> departments.contains(group.enterpriseIdentityExternalId())).map(UserGroupEntity::id).collect(java.util.stream.Collectors.toSet());
        List<UUID> sourceIds = sourceGroups.stream().map(UserGroupEntity::id).toList();
        if (!sourceIds.isEmpty())
            sql.createDelete(MEMBERSHIP).where(MEMBERSHIP.id().tenantId().eq(source.tenantId()), MEMBERSHIP.id().userId().eq(user.id()), MEMBERSHIP.id().groupId().in(sourceIds)).execute();
        for (UUID groupId : ids)
            sql.saveCommand(UserGroupAssignmentEntityDraft.$.produce(d -> d.setId(UserGroupAssignmentIdDraft.$.produce(k -> k.setTenantId(source.tenantId()).setUserId(user.id()).setGroupId(groupId))))).setMode(SaveMode.INSERT_IF_ABSENT).execute();
    }

    private void disableUser(EnterpriseIdentitySourceEntity source, String externalId) {
        if (!externalId.isBlank())
            sql.createUpdate(USER).set(USER.status(), "disabled").set(USER.updatedAt(), Instant.now()).where(USER.enterpriseIdentitySourceId().eq(source.id()), USER.enterpriseIdentityExternalId().eq(externalId)).execute();
    }

    private TaskView queue(EnterpriseIdentitySourceEntity source, String type, String eventId, Map<String, Object> payload) {
        UUID id = UuidV7.randomUuid();
        Instant now = Instant.now();
        var row = EnterpriseIdentitySyncTaskEntityDraft.$.produce(d -> d.setId(id).setTenantId(source.tenantId()).setSourceId(source.id()).setType(type).setEventId(eventId).setPayload(payload).setStatus("pending").setSummary(Map.of()).setLastError(null).setCreatedAt(now).setStartedAt(null).setFinishedAt(null));
        sql.saveCommand(row).setMode(SaveMode.INSERT_ONLY).execute();
        return taskView(row);
    }

    private void finish(UUID id, Map<String, Object> summary, String result, String error) {
        sql.createUpdate(TASK).set(TASK.status(), result).set(TASK.summary(), summary).set(TASK.lastError(), error).set(TASK.finishedAt(), Instant.now()).where(TASK.id().eq(id)).execute();
        var task = sql.findById(EnterpriseIdentitySyncTaskEntity.class, id);
        if (task != null)
            sql.createUpdate(SOURCE).set(SOURCE.lastSyncAt(), Instant.now()).set(SOURCE.lastSyncStatus(), result).set(SOURCE.lastError(), error).set(SOURCE.updatedAt(), Instant.now()).where(SOURCE.id().eq(task.sourceId())).execute();
    }

    private EnterpriseIdentitySourceEntity source(UUID tenant, UUID id) {
        return sql.createQuery(SOURCE).where(SOURCE.tenantId().eq(tenant), SOURCE.id().eq(id)).select(SOURCE).fetchOptional().orElseThrow(() -> new DomainException(ErrorCodeConstants.ENTERPRISE_IDENTITY_SOURCE_NOT_FOUND));
    }

    private Map<String, Object> decrypted(EnterpriseIdentitySourceEntity s, Map<String, Object> e) {
        if (!e.containsKey("encrypt")) return e;
        try {
            byte[] raw = Base64.getDecoder().decode(string(e.get("encrypt"))), key = MessageDigest.getInstance("SHA-256").digest(decrypt(s, "encrypt-key").getBytes(StandardCharsets.UTF_8));
            Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
            c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new IvParameterSpec(Arrays.copyOf(raw, 16)));
            return json.readValue(c.doFinal(Arrays.copyOfRange(raw, 16, raw.length)), new TypeReference<>() {
            });
        } catch (Exception ex) {
            throw new DomainException(ErrorCodeConstants.FEISHU_EVENT_DECRYPT_FAILED);
        }
    }

    private String token(EnterpriseIdentitySourceEntity s) throws Exception {
        Map<String, Object> r = post("https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal", Map.of("app_id", s.appId(), "app_secret", decrypt(s, "app-secret")));
        if (((Number) r.getOrDefault("code", -1)).intValue() != 0)
            throw new DomainException(ErrorCodeConstants.FEISHU_TOKEN_FAILED);
        return string(r.get("tenant_access_token"));
    }

    private Map<String, Object> get(String token, String path) throws Exception {
        var req = HttpRequest.newBuilder(URI.create("https://open.feishu.cn" + path)).timeout(Duration.ofSeconds(15)).header("Authorization", "Bearer " + token).GET().build();
        var r = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() != 200) throw new DomainException(ErrorCodeConstants.FEISHU_API_FAILED);
        return json.readValue(r.body(), new TypeReference<>() {
        });
    }

    private List<Map<String, Object>> paged(String token, String path) throws Exception {
        List<Map<String, Object>> result = new ArrayList<>();
        String next = null;
        do {
            Map<String, Object> page = data(get(token, path + (next == null ? "" : "&page_token=" + enc(next))));
            result.addAll(items(page));
            next = Boolean.TRUE.equals(page.get("has_more")) ? string(page.get("page_token")) : null;
        } while (next != null && !next.isBlank());
        return result;
    }

    private List<String> scopeDepartments(String token) throws Exception {
        List<String> result = new ArrayList<>();
        String next = null;
        do {
            Map<String, Object> page = data(get(token, "/open-apis/contact/v3/scopes?user_id_type=open_id&department_id_type=open_department_id&page_size=100" + (next == null ? "" : "&page_token=" + enc(next))));
            result.addAll(ids(page, "department_ids"));
            next = Boolean.TRUE.equals(page.get("has_more")) ? string(page.get("page_token")) : null;
        } while (next != null && !next.isBlank());
        return result.stream().distinct().toList();
    }

    private Map<String, Object> post(String url, Map<String, Object> body) throws Exception {
        var req = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(15)).header("Content-Type", "application/json; charset=utf-8").POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body))).build();
        var r = http.send(req, HttpResponse.BodyHandlers.ofString());
        return json.readValue(r.body(), new TypeReference<>() {
        });
    }

    private String encrypt(UUID tenant, UUID id, String part, String value) {
        return cipher.encrypt("enterprise-identity:" + tenant + ":" + id + ":" + part, value);
    }

    private String decrypt(EnterpriseIdentitySourceEntity s, String part) {
        String v = switch (part) {
            case "app-secret" -> s.encryptedAppSecret();
            case "verification-token" -> s.encryptedVerificationToken();
            default -> s.encryptedEncryptKey();
        };
        return cipher.decrypt("enterprise-identity:" + s.tenantId() + ":" + s.id() + ":" + part, v);
    }

    private String uniqueGroupName(UUID tenant, UUID self, UUID parent, String desired) {
        String base = nonBlank(desired, "未命名部门");
        String value = base;
        int n = 0;
        while (sql.createQuery(GROUP).where(GROUP.tenantId().eq(tenant), GROUP.parentId().eq(parent), GROUP.name().eq(value)).whereIf(self != null, () -> GROUP.id().ne(self)).select(GROUP.id()).exists())
            value = "飞书-" + base + (n++ == 0 ? "" : "-" + n);
        return value;
    }

    private UUID sourceGroupId(EnterpriseIdentitySourceEntity source, String externalId) {
        if (!present(externalId) || "0".equals(externalId)) return null;
        var group = sql.createQuery(GROUP).where(GROUP.enterpriseIdentitySourceId().eq(source.id()), GROUP.enterpriseIdentityExternalId().eq(externalId)).select(GROUP).fetchOneOrNull();
        return group == null ? null : group.id();
    }

    private void validate(Input x, boolean create) {
        if (x == null || (create && (!present(x.name()) || !present(x.appId()) || !present(x.appSecret()) || !present(x.verificationToken()) || !present(x.encryptKey()))))
            throw new DomainException(ErrorCodeConstants.ENTERPRISE_IDENTITY_SOURCE_INVALID);
        if (x != null && x.status() != null) status(x.status());
    }

    private static String status(String v) {
        if (!Set.of("active", "disabled").contains(v))
            throw new DomainException(ErrorCodeConstants.ENTERPRISE_IDENTITY_SOURCE_STATUS_INVALID);
        return v;
    }

    private static SourceView view(EnterpriseIdentitySourceEntity e) {
        return new SourceView(e.id(), e.name(), e.provider(), e.appId(), e.status(), e.lastSyncAt(), e.lastSyncStatus(), e.lastError(), e.createdAt(), e.updatedAt());
    }

    private static TaskView taskView(EnterpriseIdentitySyncTaskEntity e) {
        return new TaskView(e.id(), e.type(), e.status(), e.summary(), e.lastError(), e.createdAt(), e.finishedAt());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> items(Map<String, Object> data) {
        Object i = data.get("items");
        return i instanceof List<?> x ? x.stream().filter(Map.class::isInstance).map(v -> (Map<String, Object>) v).toList() : List.of();
    }

    @SuppressWarnings("unchecked")
    private static List<String> ids(Map<String, Object> data, String key) {
        Object i = data.get(key);
        return i instanceof List<?> x ? x.stream().map(EnterpriseIdentityService::string).filter(v -> !v.isBlank()).toList() : List.of();
    }

    private static Map<String, Object> data(Map<String, Object> response) {
        if (((Number) response.getOrDefault("code", -1)).intValue() != 0)
            throw new DomainException(ErrorCodeConstants.FEISHU_API_FAILED);
        return map(response.get("data"));
    }

    private static String string(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static boolean present(String s) {
        return s != null && !s.isBlank();
    }

    private static String nonBlank(String v, String fallback) {
        return present(v) ? v : fallback;
    }

    private static String trim(String v) {
        return v == null ? "同步失败" : v.substring(0, Math.min(1000, v.length()));
    }

    private static String enc(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }

    private static void increment(Map<String, Object> m, String key) {
        m.put(key, ((Number) m.getOrDefault(key, 0)).longValue() + 1);
    }

    private static boolean isDuplicate(RuntimeException e) {
        return e.getMessage() != null && (e.getMessage().contains("duplicate") || e.getMessage().contains("unique"));
    }

    private static String username(String externalId) {
        return "feishu_" + Integer.toUnsignedString(externalId.hashCode(), 36);
    }

    private static <T> T ignored(java.util.concurrent.Callable<T> callable) {
        try {
            return TenantUtils.executeIgnore(callable);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public record Input(String name, String appId, String appSecret, String verificationToken, String encryptKey,
                        String status) {
    }

    public record SourceView(UUID id, String name, String provider, String appId, String status, Instant lastSyncAt,
                             String lastSyncStatus, String lastError, Instant createdAt, Instant updatedAt) {
    }

    public record TaskView(UUID id, String type, String status, Map<String, Object> summary, String lastError,
                           Instant createdAt, Instant finishedAt) {
    }
}
