package com.easy1auth.enterpriseidentity.service;

import com.easy1auth.enterpriseidentity.constant.ErrorCodeConstants;
import com.easy1auth.enterpriseidentity.dto.*;
import com.easy1auth.enterpriseidentity.model.*;
import com.easy1auth.enterpriseidentity.repository.EnterpriseIdentityRepository;
import com.easy1auth.common.foundation.error.DomainException;
import com.easy1auth.common.foundation.id.UuidV7;
import com.easy1auth.common.foundation.web.PageData;
import com.easy1auth.poolidentity.dto.PoolUserInput;
import com.easy1auth.poolidentity.model.*;
import com.easy1auth.poolidentity.service.PoolUserService;
import com.easy1auth.security.SecurityDataCipher;
import com.easy1auth.common.foundation.util.TenantContextHolder;
import com.easy1auth.common.tenant.util.TenantUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 企业身份源同步服务：将租户配置的飞书身份源通讯录同步到本地用户 / 用户组体系， 支持全量同步与事件增量同步，敏感凭证加密存储。
 *
 * <p>注意：飞书适配器仅提供目录同步能力，不提供 SSO 登录。
 */
@Service
public class EnterpriseIdentityService {
  /** jimmer SQL 客户端 */
  private final EnterpriseIdentityRepository repository;

  /** 敏感凭证加解密器 */
  private final SecurityDataCipher cipher;

  /** JSON 序列化 / 反序列化器 */
  private final ObjectMapper json;

  /** 本地用户池服务（创建 / 更新同步过来的用户） */
  private final PoolUserService users;

  /** 飞书 Open API 的 HTTP 客户端（连接超时 8 秒） */
  private final HttpClient http =
      HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();

  public EnterpriseIdentityService(
      EnterpriseIdentityRepository repository,
      SecurityDataCipher cipher,
      ObjectMapper json,
      PoolUserService users) {
    this.repository = repository;
    this.cipher = cipher;
    this.json = json;
    this.users = users;
  }

  /** 分页查询当前租户的企业身份源列表，支持名称模糊搜索与状态过滤。 */
  @Transactional(readOnly = true)
  public PageData<EnterpriseIdentitySourceView> list(
      int page, int pageSize, String search, String status) {
    UUID tenant = TenantContextHolder.requireTenantId();
    int p = Math.max(page, 1), s = Math.min(Math.max(pageSize, 1), 100);
    var result = repository.pageSources(tenant, p, s, search, status);
    return PageData.of(
        result.items().stream().map(EnterpriseIdentityService::view).toList(),
        p,
        s,
        result.total());
  }

  /** 查询当前租户下指定身份源的详情。 */
  @Transactional(readOnly = true)
  public EnterpriseIdentitySourceView get(UUID id) {
    return view(source(TenantContextHolder.requireTenantId(), id));
  }

  /** 新建飞书企业身份源（provider=feishu），敏感凭证加密后存储，状态默认 active。 */
  @Transactional
  public EnterpriseIdentitySourceView create(EnterpriseIdentityInput input) {
    UUID tenant = TenantContextHolder.requireTenantId();
    validate(input, true);
    UUID id = UuidV7.randomUuid();
    Instant now = Instant.now();
    var row =
        EnterpriseIdentitySourceEntityDraft.$.produce(
            d ->
                d.setId(id)
                    .setTenantId(tenant)
                    .setName(input.name().strip())
                    .setProvider("feishu")
                    .setAppId(input.appId().strip())
                    .setEncryptedAppSecret(encrypt(tenant, id, "app-secret", input.appSecret()))
                    .setEncryptedVerificationToken(
                        encrypt(tenant, id, "verification-token", input.verificationToken()))
                    .setEncryptedEncryptKey(encrypt(tenant, id, "encrypt-key", input.encryptKey()))
                    .setStatus("active")
                    .setLastSyncAt(null)
                    .setLastSyncStatus(null)
                    .setLastError(null)
                    .setCreatedAt(now)
                    .setUpdatedAt(now));
    repository.saveSource(row);
    return view(row);
  }

  /** 更新身份源信息：仅更新传入的非空字段，重新传入的凭证会加密覆盖。 */
  @Transactional
  public EnterpriseIdentitySourceView update(UUID id, EnterpriseIdentityInput input) {
    UUID tenant = TenantContextHolder.requireTenantId();
    var old = source(tenant, id);
    validate(input, false);
    repository.updateSource(
        tenant,
        id,
        input,
        present(input.appSecret()) ? encrypt(tenant, id, "app-secret", input.appSecret()) : null,
        present(input.verificationToken())
            ? encrypt(tenant, id, "verification-token", input.verificationToken())
            : null,
        present(input.encryptKey()) ? encrypt(tenant, id, "encrypt-key", input.encryptKey()) : null,
        input.status() == null ? null : status(input.status()));
    return get(id);
  }

  /** 删除身份源：已导入的用户与用户组保留并转为本地管理，仅删除身份源记录本身。 */
  @Transactional
  public void delete(UUID id) {
    UUID tenant = TenantContextHolder.requireTenantId();
    source(tenant, id);
    // Imported data deliberately survives source deletion and becomes locally managed.
    repository.deleteSourceAndDetachImportedData(tenant, id);
  }

  /** 触发指定启用中身份源的全量同步，返回排队中的同步任务视图。 */
  @Transactional
  public EnterpriseIdentityTaskView sync(UUID id) {
    var source = source(TenantContextHolder.requireTenantId(), id);
    if (!"active".equals(source.status())) {
      throw new DomainException(ErrorCodeConstants.ENTERPRISE_IDENTITY_SOURCE_DISABLED);
    }
    return queue(source, "full", null, Map.of());
  }

  /** 查询指定身份源最近的同步任务列表（最多 30 条）。 */
  @Transactional(readOnly = true)
  public List<EnterpriseIdentityTaskView> tasks(UUID id) {
    UUID tenant = TenantContextHolder.requireTenantId();
    source(tenant, id);
    return repository.findRecentTasks(tenant, id).stream()
        .map(EnterpriseIdentityService::taskView)
        .toList();
  }

  /** 统计当前租户身份源的总数及启用 / 停用数量。 */
  @Transactional(readOnly = true)
  public EnterpriseIdentityStats stats() {
    UUID tenant = TenantContextHolder.requireTenantId();
    var rows = repository.findSourceStatuses(tenant);
    long active = rows.stream().filter("active"::equals).count();
    return new EnterpriseIdentityStats(rows.size(), active, rows.size() - active);
  }

  /** 飞书事件回调入口：不依赖请求中的租户头，直接按 sourceId 解析身份源， 完成事件解密与验证后入队处理（含 url_verification 挑战应答）。 */
  @Transactional
  public FeishuEventResponse acceptFeishuEvent(UUID sourceId, Map<String, Object> envelope) {
    EnterpriseIdentitySourceEntity source = ignored(() -> repository.findSource(sourceId));
    if (source == null || !"feishu".equals(source.provider())) {
      throw new DomainException(ErrorCodeConstants.ENTERPRISE_IDENTITY_SOURCE_NOT_FOUND);
    }
    Map<String, Object> event = decrypted(source, envelope);
    String token = string(event.get("token"));
    if (!MessageDigest.isEqual(
        token.getBytes(StandardCharsets.UTF_8),
        decrypt(source, "verification-token").getBytes(StandardCharsets.UTF_8))) {
      throw new DomainException(ErrorCodeConstants.FEISHU_EVENT_UNAUTHORIZED);
    }
    if ("url_verification".equals(string(event.get("type")))) {
      return new FeishuEventResponse(string(event.get("challenge")));
    }
    if (!"active".equals(source.status())) {
      return new FeishuEventResponse(null);
    }
    Map<String, Object> header = map(event.get("header"));
    String eventId = string(header.get("event_id"));
    if (eventId.isBlank()) {
      throw new DomainException(ErrorCodeConstants.FEISHU_EVENT_INVALID);
    }
    Map<String, Object> queued = new LinkedHashMap<>(event);
    queued.remove("token");
    try {
      TenantUtils.execute(
          source.tenantId(), (Runnable) () -> queue(source, "event", eventId, queued));
    } catch (RuntimeException ex) {
      if (!isDuplicate(ex)) {
        throw ex;
      }
    }
    return new FeishuEventResponse(null);
  }

  /** worker 领取待处理（pending）任务并置为 processing，返回实际领取的任务列表。 */
  @Transactional
  public List<EnterpriseIdentityTaskView> claim(int limit) {
    List<EnterpriseIdentitySyncTaskEntity> rows = repository.findPendingTasks(limit);
    List<EnterpriseIdentityTaskView> claimed = new ArrayList<>();
    for (var row : rows) {
      if (repository.claimTask(row.id())) {
        claimed.add(taskView(row));
      }
    }
    return claimed;
  }

  /** worker 处理指定任务：按任务类型执行全量或事件同步，并落最终状态。 */
  public void process(UUID taskId) {
    EnterpriseIdentitySyncTaskEntity task = ignored(() -> repository.findTask(taskId));
    if (task == null || !"processing".equals(task.status())) {
      return;
    }
    TenantUtils.execute(task.tenantId(), () -> processInTenant(task));
  }

  /** 在任务所属租户上下文中执行同步（含异常捕获与结果回写）。 */
  @Transactional
  void processInTenant(EnterpriseIdentitySyncTaskEntity task) {
    var source = source(task.tenantId(), task.sourceId());
    Map<String, Object> summary = new LinkedHashMap<>();
    try {
      if ("full".equals(task.type())) {
        full(source, summary);
      } else {
        event(source, task.payload(), summary);
      }
      boolean partial =
          summary.containsKey("skippedMissingPhone")
              || summary.containsKey("skippedEmailConflict")
              || summary.containsKey("skippedPhoneConflict");
      finish(task.id(), summary, partial ? "partial" : "succeeded", null);
    } catch (Exception ex) {
      finish(task.id(), summary, "failed", trim(ex.getMessage()));
    }
  }

  /** 全量同步：遍历授权范围内的部门树，同步用户、用户组与成员关系。 */
  private void full(EnterpriseIdentitySourceEntity source, Map<String, Object> summary)
      throws Exception {
    String token = token(source);
    Set<String> seenUsers = new HashSet<>();
    List<String> roots = scopeDepartments(token);
    if (roots.isEmpty()) {
      roots = List.of("0");
    }
    for (String root : roots) {
      syncDepartmentTree(source, token, root, null, seenUsers, summary, new HashSet<>());
    }
    summary.putIfAbsent("users", seenUsers.size());
  }

  /** 递归同步单个部门（含其成员用户与子部门），通过 traversed 记录避免重复遍历。 */
  private void syncDepartmentTree(
      EnterpriseIdentitySourceEntity source,
      String token,
      String externalId,
      UUID parent,
      Set<String> seenUsers,
      Map<String, Object> summary,
      Set<String> traversed)
      throws Exception {
    if (!traversed.add(externalId)) {
      return;
    }
    if (!"0".equals(externalId)) {
      Map<String, Object> department =
          data(
              get(
                  token,
                  "/open-apis/contact/v3/departments/"
                      + enc(externalId)
                      + "?department_id_type=open_department_id"));
      parent = upsertGroup(source, externalId, string(department.get("name")), parent);
    }
    for (Map<String, Object> user :
        paged(
            token,
            "/open-apis/contact/v3/users/find_by_department?department_id_type=open_department_id&user_id_type=open_id&department_id="
                + enc(externalId)
                + "&page_size=50")) {
      String externalUser = string(user.get("open_id"));
      if (!externalUser.isBlank()) {
        if (seenUsers.add(externalUser)) {
          upsertUser(source, externalUser, user, summary);
        }
        syncMembership(source, externalUser, ids(user, "department_ids"));
      }
    }
    for (Map<String, Object> child :
        paged(
            token,
            "/open-apis/contact/v3/departments/"
                + enc(externalId)
                + "/children?department_id_type=open_department_id&page_size=50")) {
      syncDepartmentTree(
          source,
          token,
          string(child.get("open_department_id")),
          parent,
          seenUsers,
          summary,
          traversed);
    }
  }

  /** 事件增量同步：按飞书事件类型（用户 / 部门增删改）更新本地数据。 */
  private void event(
      EnterpriseIdentitySourceEntity source,
      Map<String, Object> payload,
      Map<String, Object> summary)
      throws Exception {
    Map<String, Object> header = map(payload.get("header")),
        body = map(payload.get("event")),
        object = map(body.get("object"));
    String type = string(header.get("event_type"));
    if (type.startsWith("contact.user.")) {
      String id = string(object.get("open_id"));
      if (type.endsWith("deleted_v3")) {
        disableUser(source, id);
        summary.put("disabled", 1);
      } else if (!id.isBlank()) {
        Map<String, Object> user =
            data(
                get(
                    token(source),
                    "/open-apis/contact/v3/users/" + enc(id) + "?user_id_type=open_id"));
        upsertUser(source, id, user, summary);
        syncMembership(source, id, ids(user, "department_ids"));
      }
    } else if (type.startsWith("contact.department.") && !type.endsWith("deleted_v3")) {
      String id = string(object.get("open_department_id"));
      if (!id.isBlank()) {
        Map<String, Object> d =
            data(
                get(
                    token(source),
                    "/open-apis/contact/v3/departments/"
                        + enc(id)
                        + "?department_id_type=open_department_id"));
        upsertGroup(
            source,
            id,
            string(d.get("name")),
            sourceGroupId(source, string(d.get("parent_department_id"))));
        summary.put("departments", 1);
      }
    }
  }

  /** 新建或更新某个同步部门对应的用户组，返回其 ID。 */
  private UUID upsertGroup(
      EnterpriseIdentitySourceEntity source, String externalId, String name, UUID parent) {
    UUID tenant = source.tenantId();
    var old = repository.findGroup(tenant, source.id(), externalId).orElse(null);
    String safe = uniqueGroupName(tenant, old == null ? null : old.id(), parent, name);
    if (old == null) {
      UUID id = UuidV7.randomUuid();
      Instant now = Instant.now();
      repository.saveGroup(
          UserGroupEntityDraft.$.produce(
              d ->
                  d.setId(id)
                      .setTenantId(tenant)
                      .setName(safe)
                      .setDescription("由飞书同步管理")
                      .setType("department")
                      .setParentId(parent)
                      .setEnterpriseIdentitySourceId(source.id())
                      .setEnterpriseIdentityExternalId(externalId)
                      .setCreatedAt(now)
                      .setUpdatedAt(now)));
      return id;
    }
    repository.updateGroup(old.id(), safe, parent);
    return old.id();
  }

  /** 新建或更新一个飞书用户到本地用户池（缺手机号或发生邮箱 / 手机号冲突时跳过）。 */
  private void upsertUser(
      EnterpriseIdentitySourceEntity source,
      String externalId,
      Map<String, Object> remote,
      Map<String, Object> summary) {
    UUID tenant = source.tenantId();
    String emailValue = string(remote.get("email")).strip().toLowerCase();
    String email = emailValue.isBlank() ? null : emailValue;
    String phone = string(remote.get("mobile")).strip();
    if (phone.isBlank()) {
      increment(summary, "skippedMissingPhone");
      return;
    }
    var old = repository.findUser(tenant, source.id(), externalId).orElse(null);
    if (old == null && email != null && repository.userEmailExists(tenant, email)) {
      increment(summary, "skippedEmailConflict");
      return;
    }
    if (old == null && repository.userPhoneExists(tenant, phone)) {
      increment(summary, "skippedPhoneConflict");
      return;
    }
    String name = nonBlank(string(remote.get("name")), phone),
        avatar = string(map(remote.get("avatar")).get("avatar_240")),
        position = string(remote.get("job_title"));
    String department = "";
    List<String> deps = ids(remote, "department_ids");
    if (!deps.isEmpty()) {
      var g = repository.findSourceGroup(source.id(), deps.getFirst()).orElse(null);
      if (g != null) {
        department = g.name();
      }
    }
    if (old == null) {
      String username = username(externalId);
      var created =
          users.create(
              tenant,
              new PoolUserInput(
                  username,
                  email,
                  null,
                  phone,
                  name,
                  avatar,
                  "active",
                  department,
                  position,
                  Map.of("enterpriseIdentity", "feishu")));
      repository.linkUser(created.id(), source.id(), externalId);
      increment(summary, "createdUsers");
    } else {
      repository.updateUser(old.id(), email, name, phone, avatar, department, position);
      increment(summary, "updatedUsers");
    }
  }

  /** 按用户的部门归属重建其与同步用户组的成员关系（先清理旧关系再补齐）。 */
  private void syncMembership(
      EnterpriseIdentitySourceEntity source, String externalUserId, List<String> departments) {
    var user = repository.findUser(source.id(), externalUserId).orElse(null);
    if (user == null) {
      return;
    }
    var sourceGroups = repository.findSourceGroups(source.id());
    Set<UUID> ids =
        sourceGroups.stream()
            .filter(group -> departments.contains(group.enterpriseIdentityExternalId()))
            .map(UserGroupEntity::id)
            .collect(java.util.stream.Collectors.toSet());
    repository.replaceUserGroups(
        source.tenantId(), user.id(), sourceGroups.stream().map(UserGroupEntity::id).toList(), ids);
  }

  /** 将飞书侧已删除用户的本地状态置为 disabled。 */
  private void disableUser(EnterpriseIdentitySourceEntity source, String externalId) {
    if (!externalId.isBlank()) {
      repository.disableUser(source.id(), externalId);
    }
  }

  /** 创建一条 pending 状态的同步任务并入队，返回任务视图。 */
  private EnterpriseIdentityTaskView queue(
      EnterpriseIdentitySourceEntity source,
      String type,
      String eventId,
      Map<String, Object> payload) {
    UUID id = UuidV7.randomUuid();
    Instant now = Instant.now();
    var row =
        EnterpriseIdentitySyncTaskEntityDraft.$.produce(
            d ->
                d.setId(id)
                    .setTenantId(source.tenantId())
                    .setSourceId(source.id())
                    .setType(type)
                    .setEventId(eventId)
                    .setPayload(payload)
                    .setStatus("pending")
                    .setSummary(Map.of())
                    .setLastError(null)
                    .setCreatedAt(now)
                    .setStartedAt(null)
                    .setFinishedAt(null));
    repository.saveTask(row);
    return taskView(row);
  }

  /** 结束任务并回写身份源的最近同步时间与结果。 */
  private void finish(UUID id, Map<String, Object> summary, String result, String error) {
    repository.finishTask(id, summary, result, error);
  }

  /** 按租户与 ID 查询身份源，不存在时抛出领域异常。 */
  private EnterpriseIdentitySourceEntity source(UUID tenant, UUID id) {
    return repository
        .findSource(tenant, id)
        .orElseThrow(
            () -> new DomainException(ErrorCodeConstants.ENTERPRISE_IDENTITY_SOURCE_NOT_FOUND));
  }

  /** 解密飞书事件包络中的 encrypt 字段；未加密时原样返回。 */
  private Map<String, Object> decrypted(EnterpriseIdentitySourceEntity s, Map<String, Object> e) {
    if (!e.containsKey("encrypt")) {
      return e;
    }
    try {
      byte[] raw = Base64.getDecoder().decode(string(e.get("encrypt"))),
          key =
              MessageDigest.getInstance("SHA-256")
                  .digest(decrypt(s, "encrypt-key").getBytes(StandardCharsets.UTF_8));
      Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
      c.init(
          Cipher.DECRYPT_MODE,
          new SecretKeySpec(key, "AES"),
          new IvParameterSpec(Arrays.copyOf(raw, 16)));
      return json.readValue(
          c.doFinal(Arrays.copyOfRange(raw, 16, raw.length)), new TypeReference<>() {});
    } catch (Exception ex) {
      throw new DomainException(ErrorCodeConstants.FEISHU_EVENT_DECRYPT_FAILED);
    }
  }

  /** 获取飞书 tenant_access_token，失败时抛出领域异常。 */
  private String token(EnterpriseIdentitySourceEntity s) throws Exception {
    Map<String, Object> r =
        post(
            "https://open.feishu.cn/open-apis/auth/v3/tenant_access_token/internal",
            Map.of("app_id", s.appId(), "app_secret", decrypt(s, "app-secret")));
    if (((Number) r.getOrDefault("code", -1)).intValue() != 0) {
      throw new DomainException(ErrorCodeConstants.FEISHU_TOKEN_FAILED);
    }
    return string(r.get("tenant_access_token"));
  }

  private Map<String, Object> get(String token, String path) throws Exception {
    var req =
        HttpRequest.newBuilder(URI.create("https://open.feishu.cn" + path))
            .timeout(Duration.ofSeconds(15))
            .header("Authorization", "Bearer " + token)
            .GET()
            .build();
    var r = http.send(req, HttpResponse.BodyHandlers.ofString());
    if (r.statusCode() != 200) {
      throw new DomainException(ErrorCodeConstants.FEISHU_API_FAILED);
    }
    return json.readValue(r.body(), new TypeReference<>() {});
  }

  private List<Map<String, Object>> paged(String token, String path) throws Exception {
    List<Map<String, Object>> result = new ArrayList<>();
    String next = null;
    do {
      Map<String, Object> page =
          data(get(token, path + (next == null ? "" : "&page_token=" + enc(next))));
      result.addAll(items(page));
      next = Boolean.TRUE.equals(page.get("has_more")) ? string(page.get("page_token")) : null;
    } while (next != null && !next.isBlank());
    return result;
  }

  /** 分页拉取授权范围内的部门 ID 列表。 */
  private List<String> scopeDepartments(String token) throws Exception {
    List<String> result = new ArrayList<>();
    String next = null;
    do {
      Map<String, Object> page =
          data(
              get(
                  token,
                  "/open-apis/contact/v3/scopes?user_id_type=open_id&department_id_type=open_department_id&page_size=100"
                      + (next == null ? "" : "&page_token=" + enc(next))));
      result.addAll(ids(page, "department_ids"));
      next = Boolean.TRUE.equals(page.get("has_more")) ? string(page.get("page_token")) : null;
    } while (next != null && !next.isBlank());
    return result.stream().distinct().toList();
  }

  private Map<String, Object> post(String url, Map<String, Object> body) throws Exception {
    var req =
        HttpRequest.newBuilder(URI.create(url))
            .timeout(Duration.ofSeconds(15))
            .header("Content-Type", "application/json; charset=utf-8")
            .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
            .build();
    var r = http.send(req, HttpResponse.BodyHandlers.ofString());
    return json.readValue(r.body(), new TypeReference<>() {});
  }

  /** 使用安全上下文（租户 + ID + 字段名）加密凭证明文。 */
  private String encrypt(UUID tenant, UUID id, String part, String value) {
    return cipher.encrypt("enterprise-identity:" + tenant + ":" + id + ":" + part, value);
  }

  /** 使用安全上下文解密指定字段（app-secret / verification-token / encrypt-key）的凭证密文。 */
  private String decrypt(EnterpriseIdentitySourceEntity s, String part) {
    String v =
        switch (part) {
          case "app-secret" -> s.encryptedAppSecret();
          case "verification-token" -> s.encryptedVerificationToken();
          default -> s.encryptedEncryptKey();
        };
    return cipher.decrypt("enterprise-identity:" + s.tenantId() + ":" + s.id() + ":" + part, v);
  }

  /** 生成同级用户组下唯一的名称，冲突时添加"飞书-"前缀与序号。 */
  private String uniqueGroupName(UUID tenant, UUID self, UUID parent, String desired) {
    String base = nonBlank(desired, "未命名部门");
    String value = base;
    int n = 0;
    while (repository.groupNameExists(tenant, self, parent, value)) {
      value = "飞书-" + base + (n++ == 0 ? "" : "-" + n);
    }
    return value;
  }

  private UUID sourceGroupId(EnterpriseIdentitySourceEntity source, String externalId) {
    if (!present(externalId) || "0".equals(externalId)) {
      return null;
    }
    var group = repository.findSourceGroup(source.id(), externalId).orElse(null);
    return group == null ? null : group.id();
  }

  private void validate(EnterpriseIdentityInput x, boolean create) {
    if (x == null
        || (create
            && (!present(x.name())
                || !present(x.appId())
                || !present(x.appSecret())
                || !present(x.verificationToken())
                || !present(x.encryptKey())))) {
      throw new DomainException(ErrorCodeConstants.ENTERPRISE_IDENTITY_SOURCE_INVALID);
    }
    if (x != null && x.status() != null) {
      status(x.status());
    }
  }

  private static String status(String v) {
    if (!Set.of("active", "disabled").contains(v)) {
      throw new DomainException(ErrorCodeConstants.ENTERPRISE_IDENTITY_SOURCE_STATUS_INVALID);
    }
    return v;
  }

  /** 将身份源实体转换为视图 DTO。 */
  private static EnterpriseIdentitySourceView view(EnterpriseIdentitySourceEntity e) {
    return new EnterpriseIdentitySourceView(
        e.id(),
        e.name(),
        e.provider(),
        e.appId(),
        e.status(),
        e.lastSyncAt(),
        e.lastSyncStatus(),
        e.lastError(),
        e.createdAt(),
        e.updatedAt());
  }

  /** 将同步任务实体转换为视图 DTO。 */
  private static EnterpriseIdentityTaskView taskView(EnterpriseIdentitySyncTaskEntity e) {
    return new EnterpriseIdentityTaskView(
        e.id(), e.type(), e.status(), e.summary(), e.lastError(), e.createdAt(), e.finishedAt());
  }

  @SuppressWarnings("unchecked")
  private static Map<String, Object> map(Object o) {
    return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
  }

  @SuppressWarnings("unchecked")
  private static List<Map<String, Object>> items(Map<String, Object> data) {
    Object i = data.get("items");
    return i instanceof List<?> x
        ? x.stream().filter(Map.class::isInstance).map(v -> (Map<String, Object>) v).toList()
        : List.of();
  }

  @SuppressWarnings("unchecked")
  private static List<String> ids(Map<String, Object> data, String key) {
    Object i = data.get(key);
    return i instanceof List<?> x
        ? x.stream().map(EnterpriseIdentityService::string).filter(v -> !v.isBlank()).toList()
        : List.of();
  }

  /** 校验飞书响应 code 并返回 data 字段，失败时抛出领域异常。 */
  private static Map<String, Object> data(Map<String, Object> response) {
    if (((Number) response.getOrDefault("code", -1)).intValue() != 0) {
      throw new DomainException(ErrorCodeConstants.FEISHU_API_FAILED);
    }
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
    return e.getMessage() != null
        && (e.getMessage().contains("duplicate") || e.getMessage().contains("unique"));
  }

  /** 由飞书 open_id 生成稳定的本地用户名（feishu_ 前缀）。 */
  private static String username(String externalId) {
    return "feishu_" + Integer.toUnsignedString(externalId.hashCode(), 36);
  }

  /** 忽略租户上下文执行回调（用于回调入口等不信任请求租户头的场景）。 */
  private static <T> T ignored(java.util.concurrent.Callable<T> callable) {
    try {
      return TenantUtils.executeIgnore(callable);
    } catch (Exception ex) {
      throw new IllegalStateException(ex);
    }
  }
}
