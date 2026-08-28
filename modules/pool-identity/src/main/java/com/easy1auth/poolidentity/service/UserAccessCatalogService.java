package com.easy1auth.poolidentity.service;

import com.easy1auth.poolidentity.model.PoolPermissionEntity;
import com.easy1auth.poolidentity.model.PoolRoleEntity;
import com.easy1auth.tenant.TenantContextHolder;

import com.easy1auth.poolidentity.PoolUserView;
import com.easy1auth.poolidentity.model.PoolUserEntity;
import com.easy1auth.poolidentity.model.PoolUserEntityTable;
import com.easy1auth.infrastructure.foundation.error.DomainException;
import com.easy1auth.infrastructure.foundation.id.UuidV7;
import com.easy1auth.infrastructure.foundation.web.PageData;
import com.easy1auth.poolidentity.ErrorCodeConstants;
import com.easy1auth.poolidentity.model.*;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

/**
 * 用户访问控制目录服务：管理 pool_user 的角色、权限及其层级与分配关系。
 *
 * <p>角色（{@link PoolRoleEntity}）按租户隔离，支持数据范围（dataScope）与
 * 权限集合配置，内置角色（type=system）不可修改或删除；权限（
 * {@link PoolPermissionEntity}）区分 menu / operation / data 三类，可组成树形结构。
 * 租户首次访问权限功能时，会按 {@code PRESETS} 自动初始化预置权限。
 * 本服务仅服务于 pool_user 的访问控制，与管理端（admin_user）角色体系相互独立。</p>
 */
@Service
public class UserAccessCatalogService {
    /** 角色表（pool_role）静态描述符 */
    private static final PoolRoleEntityTable ROLE = PoolRoleEntityTable.$;
    /** 权限表（pool_permission）静态描述符 */
    private static final PoolPermissionEntityTable PERMISSION = PoolPermissionEntityTable.$;
    /** 用户-角色分配表（pool_user_role）静态描述符 */
    private static final UserRoleAssignmentEntityTable ASSIGNMENT = UserRoleAssignmentEntityTable.$;
    /** pool_user 用户表静态描述符 */
    private static final PoolUserEntityTable USER = PoolUserEntityTable.$;
    /** jimmer SQL 客户端 */
    private final JSqlClient sql;
    /** JDBC 客户端（用于预置权限的批量初始化写入） */
    private final JdbcClient db;
    /** 每个租户首次使用权限功能时自动初始化的预置权限清单 */
    private static final List<PermissionSeed> PRESETS = List.of(
            new PermissionSeed("user:read", "查看用户", "operation", "user", "read"), new PermissionSeed("user:create", "创建用户", "operation", "user", "create"), new PermissionSeed("user:update", "编辑用户", "operation", "user", "update"), new PermissionSeed("user:delete", "删除用户", "operation", "user", "delete"),
            new PermissionSeed("group:read", "查看用户组", "operation", "group", "read"), new PermissionSeed("group:create", "创建用户组", "operation", "group", "create"), new PermissionSeed("group:update", "编辑用户组", "operation", "group", "update"), new PermissionSeed("group:delete", "删除用户组", "operation", "group", "delete"),
            new PermissionSeed("position:read", "查看岗位", "operation", "position", "read"), new PermissionSeed("position:create", "创建岗位", "operation", "position", "create"), new PermissionSeed("position:update", "编辑岗位", "operation", "position", "update"), new PermissionSeed("position:delete", "删除岗位", "operation", "position", "delete"),
            new PermissionSeed("role:read", "查看角色", "operation", "role", "read"), new PermissionSeed("role:create", "创建角色", "operation", "role", "create"), new PermissionSeed("role:update", "编辑角色", "operation", "role", "update"), new PermissionSeed("role:delete", "删除角色", "operation", "role", "delete"), new PermissionSeed("role:assign", "分配角色", "operation", "role", "assign"),
            new PermissionSeed("permission:read", "查看权限", "operation", "permission", "read"), new PermissionSeed("permission:create", "创建权限", "operation", "permission", "create"), new PermissionSeed("permission:update", "编辑权限", "operation", "permission", "update"), new PermissionSeed("permission:delete", "删除权限", "operation", "permission", "delete"),
            new PermissionSeed("data:all", "全部数据", "data", "data", "all"), new PermissionSeed("data:department", "本部门数据", "data", "data", "department"), new PermissionSeed("data:department-sub", "本部门及下级数据", "data", "data", "department_and_sub"), new PermissionSeed("data:self", "仅本人数据", "data", "data", "self"),
            new PermissionSeed("menu:user", "用户管理", "menu", "menu", "user"), new PermissionSeed("menu:group", "用户组管理", "menu", "menu", "group"), new PermissionSeed("menu:position", "岗位管理", "menu", "menu", "position"), new PermissionSeed("menu:role", "角色管理", "menu", "menu", "role"), new PermissionSeed("menu:permission", "权限管理", "menu", "menu", "permission"));

    public UserAccessCatalogService(JSqlClient sql, JdbcClient db) {
        this.sql = sql;
        this.db = db;
    }

    /** 分页查询租户下的角色，可按名称/编码模糊搜索、按类型过滤，按创建时间倒序。 */
    @Transactional(readOnly = true)
    public PageData<RoleView> roles(UUID tenant, int page, int pageSize, String search, String type) {
        int p = Math.max(1, page), size = Math.min(100, Math.max(1, pageSize));
        var query = sql.createQuery(ROLE).where(ROLE.tenantId().eq(tenant))
                .whereIf(search != null, () -> Predicate.or(ROLE.name().ilike(search, LikeMode.ANYWHERE), ROLE.code().ilike(search, LikeMode.ANYWHERE)))
                .whereIf(type != null, () -> ROLE.type().eq(type)).orderBy(ROLE.createdAt().desc()).select(ROLE);
        long total = query.fetchUnlimitedCount();
        return PageData.of(query.limit(size, (long) (p - 1) * size).execute().stream().map(this::roleView).toList(), p, size, total);
    }

    /** 查询租户下单个角色的详情视图。 */
    @Transactional(readOnly = true)
    public RoleView role(UUID tenant, UUID id) {
        return roleView(roleEntity(tenant, id));
    }

    /** 创建自定义角色（type=custom）：校验名称/编码/数据范围与父角色合法性。 */
    @Transactional
    public RoleView createRole(UUID tenant, RoleInput in) {
        if ("system".equals(in.type())) {
            throw new DomainException(ErrorCodeConstants.SYSTEM_ROLE_RESERVED);
        }
        validateRole(in.name(), in.code(), in.dataScope());
        validateRoleParent(tenant, null, in.parentId());
        Instant now = Instant.now();
        var e = PoolRoleEntityDraft.$.produce(d -> d.setId(UuidV7.randomUuid()).setTenantId(tenant)
                .setName(in.name()).setCode(in.code()).setDescription(in.description()).setType(in.type() == null ? "custom" : in.type())
                .setPermissions(in.permissions() == null ? Map.of() : in.permissions()).setDataScope(in.dataScope() == null ? "self" : in.dataScope())
                .setParentId(in.parentId()).setCreatedAt(now).setUpdatedAt(now));
        sql.saveCommand(e).setMode(SaveMode.INSERT_ONLY).execute();
        return roleView(e);
    }

    /** 更新自定义角色的名称、数据范围、描述、权限集合与父角色；内置角色不可修改。 */
    @Transactional
    public RoleView updateRole(UUID tenant, UUID id, RoleInput in) {
        var old = roleEntity(tenant, id);
        if ("system".equals(old.type())) {
            throw new DomainException(ErrorCodeConstants.SYSTEM_ROLE_IMMUTABLE_UPDATE);
        }
        String name = in.name() == null ? old.name() : in.name(), scope = in.dataScope() == null ? old.dataScope() : in.dataScope();
        validateRole(name, old.code(), scope);
        validateRoleParent(tenant, id, in.parentId());
        var u = sql.createUpdate(ROLE).set(ROLE.name(), name).set(ROLE.dataScope(), scope).set(ROLE.updatedAt(), Instant.now()).where(ROLE.id().eq(id), ROLE.tenantId().eq(tenant));
        if (in.description() != null) {
            u.set(ROLE.description(), in.description());
        }
        if (in.permissions() != null) {
            u.set(ROLE.permissions(), in.permissions());
        }
        if (in.parentId() != null) {
            u.set(ROLE.parentId(), in.parentId());
        }
        u.execute();
        return role(tenant, id);
    }

    /** 删除自定义角色：内置角色、仍分配有用户或存在子角色的角色不可删除。 */
    @Transactional
    public void deleteRole(UUID tenant, UUID id) {
        var role = roleEntity(tenant, id);
        if ("system".equals(role.type())) {
            throw new DomainException(ErrorCodeConstants.SYSTEM_ROLE_IMMUTABLE_DELETE);
        }
        if (assignmentCount(tenant, id) > 0) {
            throw new DomainException(ErrorCodeConstants.ROLE_HAS_USERS);
        }
        if (sql.createQuery(ROLE).where(ROLE.tenantId().eq(tenant), ROLE.parentId().eq(id)).select(ROLE.id()).exists()) {
            throw new DomainException(ErrorCodeConstants.ROLE_HAS_CHILDREN);
        }
        sql.createDelete(ROLE).where(ROLE.id().eq(id), ROLE.tenantId().eq(tenant)).execute();
    }

    /** 构建租户下的角色树（按父角色 parentId 组织层级）。 */
    @Transactional(readOnly = true)
    public List<RoleTree> roleTree(UUID tenant) {
        var roles = sql.createQuery(ROLE).where(ROLE.tenantId().eq(tenant)).orderBy(ROLE.createdAt()).select(ROLE).execute();
        Map<UUID, MutableRoleTree> nodes = new LinkedHashMap<>();
        roles.forEach(r -> nodes.put(r.id(), new MutableRoleTree(roleView(r))));
        List<MutableRoleTree> roots = new ArrayList<>();
        nodes.values().forEach(n -> {
            var p = n.role.parentId() == null ? null : nodes.get(n.role.parentId());
            if (p == null) {
                roots.add(n);
            } else {
                p.children.add(n);
            }
        });
        return roots.stream().map(MutableRoleTree::freeze).toList();
    }

    /** 统计租户下的角色总数、内置/自定义角色数以及已分配角色的去重用户数。 */
    @Transactional(readOnly = true)
    public RoleStats roleStats(UUID tenant) {
        var roles = sql.createQuery(ROLE).where(ROLE.tenantId().eq(tenant)).select(ROLE).execute();
        var roleIds = roles.stream().map(PoolRoleEntity::id).toList();
        var assigned = roleIds.isEmpty() ? 0L : sql.createQuery(ASSIGNMENT).where(ASSIGNMENT.id().tenantId().eq(tenant), ASSIGNMENT.id().roleId().in(roleIds)).select(ASSIGNMENT.id().userId()).execute().stream().distinct().count();
        return new RoleStats(roles.size(), roles.stream().filter(r -> "system".equals(r.type())).count(),
                roles.stream().filter(r -> "custom".equals(r.type())).count(), assigned);
    }

    /** 查询指定角色下的用户列表，可按用户名/邮箱/姓名模糊搜索。 */
    @Transactional(readOnly = true)
    public RoleUsers roleUsers(UUID tenant, UUID roleId, String search) {
        roleEntity(tenant, roleId);
        var userIds = sql.createQuery(ASSIGNMENT).where(ASSIGNMENT.id().tenantId().eq(tenant), ASSIGNMENT.id().roleId().eq(roleId)).select(ASSIGNMENT.id().userId()).execute();
        if (userIds.isEmpty()) {
            return new RoleUsers(List.of(), 0);
        }
        var users = sql.createQuery(USER).where(USER.tenantId().eq(tenant), USER.id().in(userIds))
                .whereIf(search != null, () -> Predicate.or(USER.username().ilike(search, LikeMode.ANYWHERE), USER.email().ilike(search, LikeMode.ANYWHERE), USER.name().ilike(search, LikeMode.ANYWHERE)))
                .orderBy(USER.createdAt().desc()).select(USER).execute().stream().map(this::userView).toList();
        return new RoleUsers(users, users.size());
    }

    /** 批量给角色分配用户（已存在的分配自动跳过）。 */
    @Transactional
    public void assignUsers(UUID tenant, UUID roleId, Collection<UUID> users) {
        roleEntity(tenant, roleId);
        users.forEach(id -> userEntity(tenant, id));
        users.forEach(id -> insertAssignment(tenant, id, roleId));
    }

    /** 批量移除角色下的用户分配。 */
    @Transactional
    public void removeUsers(UUID tenant, UUID roleId, Collection<UUID> users) {
        roleEntity(tenant, roleId);
        users.forEach(id -> userEntity(tenant, id));
        users.forEach(id -> sql.createDelete(ASSIGNMENT).where(ASSIGNMENT.id().tenantId().eq(tenant), ASSIGNMENT.id().userId().eq(id), ASSIGNMENT.id().roleId().eq(roleId)).execute());
    }

    /** 查询用户当前分配的全部角色。 */
    @Transactional(readOnly = true)
    public List<RoleView> rolesForUser(UUID tenant, UUID userId) {
        userEntity(tenant, userId);
        var ids = sql.createQuery(ASSIGNMENT).where(ASSIGNMENT.id().tenantId().eq(tenant), ASSIGNMENT.id().userId().eq(userId)).select(ASSIGNMENT.id().roleId()).execute();
        return ids.isEmpty() ? List.of() : sql.createQuery(ROLE).where(ROLE.tenantId().eq(tenant), ROLE.id().in(ids)).select(ROLE).execute().stream().map(this::roleView).toList();
    }

    /** 替换用户的全部角色分配：先删除旧的分配，再批量插入新的分配。 */
    @Transactional
    public void replaceUserRoles(UUID tenant, UUID userId, Collection<UUID> roleIds) {
        userEntity(tenant, userId);
        roleIds.forEach(id -> roleEntity(tenant, id));
        sql.createDelete(ASSIGNMENT).where(ASSIGNMENT.id().tenantId().eq(tenant), ASSIGNMENT.id().userId().eq(userId)).execute();
        roleIds.forEach(id -> insertAssignment(tenant, userId, id));
    }

    /** 计算用户的有效数据范围：按从大到小优先级取 all / department_and_sub / department，否则为 self。 */
    @Transactional(readOnly = true)
    public String effectiveDataScope(UUID tenant, UUID userId) {
        var scopes = rolesForUser(tenant, userId).stream().map(RoleView::dataScope).collect(java.util.stream.Collectors.toSet());
        if (scopes.contains("all")) {
            return "all";
        }
        if (scopes.contains("department_and_sub")) {
            return "department_and_sub";
        }
        if (scopes.contains("department")) {
            return "department";
        }
        return "self";
    }

    /** 分页查询租户下的权限（首次访问时自动初始化预置权限），支持名称/编码/资源搜索及类型/资源过滤。 */
    @Transactional
    public PageData<PermissionView> permissions(UUID tenant, int page, int pageSize, String search, String type, String resource) {
        ensurePresetPermissions(tenant);
        int p = Math.max(1, page), size = Math.min(200, Math.max(1, pageSize));
        var query = sql.createQuery(PERMISSION).where(PERMISSION.tenantId().eq(tenant))
                .whereIf(search != null && !search.isBlank(), () -> Predicate.or(PERMISSION.name().ilike(search, LikeMode.ANYWHERE), PERMISSION.code().ilike(search, LikeMode.ANYWHERE), PERMISSION.resource().ilike(search, LikeMode.ANYWHERE)))
                .whereIf(type != null && !type.isBlank(), () -> PERMISSION.type().eq(type)).whereIf(resource != null && !resource.isBlank(), () -> PERMISSION.resource().eq(resource))
                .orderBy(PERMISSION.resource(), PERMISSION.code()).select(PERMISSION);
        long total = query.fetchUnlimitedCount();
        return PageData.of(query.limit(size, (long) (p - 1) * size).execute().stream().map(this::permissionView).toList(), p, size, total);
    }

    /** 查询租户下单个权限的详情视图。 */
    @Transactional(readOnly = true)
    public PermissionView permission(UUID tenant, UUID id) {
        return permissionView(permissionEntity(tenant, id));
    }

    /** 创建权限：校验名称/编码/类型/资源/动作与父权限合法性。 */
    @Transactional
    public PermissionView createPermission(UUID tenant, PermissionInput in) {
        validatePermission(in.name(), in.code(), in.type(), in.resource(), in.action());
        validatePermissionParent(tenant, null, in.parentId());
        Instant now = Instant.now();
        var e = PoolPermissionEntityDraft.$.produce(d -> d.setId(UuidV7.randomUuid()).setTenantId(tenant).setCode(in.code()).setName(in.name())
                .setDescription(in.description()).setType(in.type() == null ? "operation" : in.type()).setParentId(in.parentId()).setResource(in.resource()).setAction(in.action()).setCreatedAt(now).setUpdatedAt(now));
        sql.saveCommand(e).setMode(SaveMode.INSERT_ONLY).execute();
        return permissionView(e);
    }

    /** 更新权限的名称、类型、资源、动作、描述与父权限（仅更新传入的非空字段）。 */
    @Transactional
    public PermissionView updatePermission(UUID tenant, UUID id, PermissionInput in) {
        var old = permissionEntity(tenant, id);
        String name = in.name() == null ? old.name() : in.name(), type = in.type() == null ? old.type() : in.type(), resource = in.resource() == null ? old.resource() : in.resource(), action = in.action() == null ? old.action() : in.action();
        validatePermission(name, old.code(), type, resource, action);
        validatePermissionParent(tenant, id, in.parentId());
        var u = sql.createUpdate(PERMISSION).set(PERMISSION.name(), name).set(PERMISSION.type(), type).set(PERMISSION.resource(), resource).set(PERMISSION.action(), action).set(PERMISSION.updatedAt(), Instant.now()).where(PERMISSION.id().eq(id), PERMISSION.tenantId().eq(tenant));
        if (in.description() != null) {
            u.set(PERMISSION.description(), in.description());
        }
        if (in.parentId() != null) {
            u.set(PERMISSION.parentId(), in.parentId());
        }
        u.execute();
        return permission(tenant, id);
    }

    /** 删除权限；存在子权限的权限不可删除。 */
    @Transactional
    public void deletePermission(UUID tenant, UUID id) {
        permissionEntity(tenant, id);
        if (sql.createQuery(PERMISSION).where(PERMISSION.tenantId().eq(tenant), PERMISSION.parentId().eq(id)).select(PERMISSION.id()).exists()) {
            throw new DomainException(ErrorCodeConstants.PERMISSION_HAS_CHILDREN);
        }
        sql.createDelete(PERMISSION).where(PERMISSION.id().eq(id), PERMISSION.tenantId().eq(tenant)).execute();
    }

    /** 构建租户下的权限树（按父权限 parentId 组织层级，首次访问自动初始化预置权限）。 */
    @Transactional
    public List<PermissionTree> permissionTree(UUID tenant) {
        ensurePresetPermissions(tenant);
        var rows = sql.createQuery(PERMISSION).where(PERMISSION.tenantId().eq(tenant)).orderBy(PERMISSION.type(), PERMISSION.resource(), PERMISSION.code()).select(PERMISSION).execute();
        Map<UUID, MutablePermissionTree> nodes = new LinkedHashMap<>();
        rows.forEach(x -> nodes.put(x.id(), new MutablePermissionTree(permissionView(x))));
        List<MutablePermissionTree> roots = new ArrayList<>();
        nodes.values().forEach(n -> {
            var p = n.permission.parentId() == null ? null : nodes.get(n.permission.parentId());
            if (p == null) {
                roots.add(n);
            } else {
                p.children.add(n);
            }
        });
        return roots.stream().map(MutablePermissionTree::freeze).toList();
    }

    /** 统计租户下的权限总数及 menu / operation / data 三类数量。 */
    @Transactional
    public PermissionStats permissionStats(UUID tenant) {
        ensurePresetPermissions(tenant);
        var rows = sql.createQuery(PERMISSION).where(PERMISSION.tenantId().eq(tenant)).select(PERMISSION.type()).execute();
        return new PermissionStats(rows.size(), rows.stream().filter("menu"::equals).count(), rows.stream().filter("operation"::equals).count(), rows.stream().filter("data"::equals).count());
    }

    private void insertAssignment(UUID tenant, UUID user, UUID role) {
        var id = UserRoleAssignmentIdDraft.$.produce(d -> d.setTenantId(tenant).setUserId(user).setRoleId(role));
        sql.saveCommand(UserRoleAssignmentEntityDraft.$.produce(d -> d.setId(id))).setMode(SaveMode.INSERT_IF_ABSENT).execute();
    }

    /** 确保租户的预置权限已初始化：缺失的预置项通过原生 SQL 批量插入（幂等）。 */
    private void ensurePresetPermissions(UUID tenant) {
        var presetCodes = PRESETS.stream().map(PermissionSeed::code).toList();
        var existingCodes = new HashSet<>(sql.createQuery(PERMISSION)
                .where(PERMISSION.tenantId().eq(tenant), PERMISSION.code().in(presetCodes))
                .select(PERMISSION.code())
                .execute());
        if (existingCodes.size() == PRESETS.size()) {
            return;
        }

        // PostgreSQL's timestamptz parameter needs an explicit JDBC type. Passing
        // an Instant without one makes the driver unable to infer the SQL type.
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        PRESETS.stream().filter(p -> !existingCodes.contains(p.code())).forEach(p -> db.sql("""
                        insert into pool_permission
                            (id, tenant_id, code, name, type, resource, action, created_at, updated_at)
                        values
                            (:id, :tenantId, :code, :name, :type, :resource, :action, :createdAt, :updatedAt)
                        on conflict (tenant_id, code) do nothing
                        """)
                .param("id", UuidV7.randomUuid())
                .param("tenantId", tenant)
                .param("code", p.code())
                .param("name", p.name())
                .param("type", p.type())
                .param("resource", p.resource())
                .param("action", p.action())
                .param("createdAt", now, Types.TIMESTAMP_WITH_TIMEZONE)
                .param("updatedAt", now, Types.TIMESTAMP_WITH_TIMEZONE)
                .update());
    }

    private PoolRoleEntity roleEntity(UUID tenant, UUID id) {
        return sql.createQuery(ROLE).where(ROLE.id().eq(id), ROLE.tenantId().eq(tenant)).select(ROLE).fetchOptional().orElseThrow(() -> new DomainException(ErrorCodeConstants.USER_ROLE_NOT_FOUND));
    }

    private PoolPermissionEntity permissionEntity(UUID tenant, UUID id) {
        return sql.createQuery(PERMISSION).where(PERMISSION.id().eq(id), PERMISSION.tenantId().eq(tenant)).select(PERMISSION).fetchOptional().orElseThrow(() -> new DomainException(ErrorCodeConstants.USER_PERMISSION_NOT_FOUND));
    }

    private PoolUserEntity userEntity(UUID tenant, UUID id) {
        return sql.createQuery(USER).where(USER.id().eq(id), USER.tenantId().eq(tenant)).select(USER).fetchOptional().orElseThrow(() -> new DomainException(ErrorCodeConstants.USER_ACCESS_USER_NOT_FOUND));
    }

    private long assignmentCount(UUID tenant, UUID role) {
        return sql.createQuery(ASSIGNMENT).where(ASSIGNMENT.id().tenantId().eq(tenant), ASSIGNMENT.id().roleId().eq(role)).select(ASSIGNMENT.id()).fetchUnlimitedCount();
    }

    private RoleView roleView(PoolRoleEntity r) {
        return new RoleView(r.id(), r.tenantId(), r.name(), r.code(), r.description(), r.type(), r.permissions(), r.dataScope(), r.parentId(), r.createdAt(), r.updatedAt(), assignmentCount(r.tenantId(), r.id()), roleParent(r.tenantId(), r.parentId()));
    }

    private PermissionView permissionView(PoolPermissionEntity p) {
        return new PermissionView(p.id(), p.tenantId(), p.code(), p.name(), p.description(), p.type(), p.resource(), p.action(), p.parentId(), p.createdAt(), p.updatedAt(), permissionParent(p.tenantId(), p.parentId()));
    }

    private ParentSummary roleParent(UUID tenant, UUID id) {
        if (id == null) {
            return null;
        }
        var p = sql.createQuery(ROLE).where(ROLE.id().eq(id), ROLE.tenantId().eq(tenant)).select(ROLE).fetchOneOrNull();
        return p == null ? null : new ParentSummary(p.id(), p.name(), p.code());
    }

    private ParentSummary permissionParent(UUID tenant, UUID id) {
        if (id == null) {
            return null;
        }
        var p = sql.createQuery(PERMISSION).where(PERMISSION.id().eq(id), PERMISSION.tenantId().eq(tenant)).select(PERMISSION).fetchOneOrNull();
        return p == null ? null : new ParentSummary(p.id(), p.name(), p.code());
    }

    private PoolUserView userView(PoolUserEntity e) {
        return new PoolUserView(e.id(), e.tenantId(), e.username(), e.email(), e.phone(), e.name(), e.avatar(), e.status(), e.emailVerified(), e.phoneVerified(), e.department(), e.position(), e.customAttributes(), e.lastLoginAt(), e.createdAt(), e.updatedAt());
    }

    private void validateRoleParent(UUID tenant, UUID self, UUID parent) {
        if (parent == null) {
            return;
        }
        if (parent.equals(self)) {
            throw new DomainException(ErrorCodeConstants.ROLE_PARENT_SELF);
        }
        var p = roleEntity(tenant, parent);
        Set<UUID> seen = new HashSet<>();
        while (p.parentId() != null) {
            if (!seen.add(p.id()) || p.parentId().equals(self)) {
                throw new DomainException(ErrorCodeConstants.ROLE_CYCLE);
            }
            p = roleEntity(tenant, p.parentId());
        }
    }

    private void validatePermissionParent(UUID tenant, UUID self, UUID parent) {
        if (parent == null) {
            return;
        }
        if (parent.equals(self)) {
            throw new DomainException(ErrorCodeConstants.PERMISSION_PARENT_SELF);
        }
        var p = permissionEntity(tenant, parent);
        Set<UUID> seen = new HashSet<>();
        while (p.parentId() != null) {
            if (!seen.add(p.id()) || p.parentId().equals(self)) {
                throw new DomainException(ErrorCodeConstants.PERMISSION_CYCLE);
            }
            p = permissionEntity(tenant, p.parentId());
        }
    }

    private void validateRole(String name, String code, String scope) {
        if (name == null || name.isBlank() || code == null || code.isBlank() || !Set.of("all", "department", "department_and_sub", "self").contains(scope == null ? "self" : scope)) {
            throw new DomainException(ErrorCodeConstants.ROLE_INVALID);
        }
    }

    private void validatePermission(String name, String code, String type, String resource, String action) {
        if (name == null || code == null || resource == null || action == null || !Set.of("menu", "operation", "data").contains(type == null ? "operation" : type)) {
            throw new DomainException(ErrorCodeConstants.PERMISSION_INVALID);
        }
    }

    /** 从当前租户上下文取租户 ID 后分页查询角色。 */
    @Transactional(readOnly = true)
    public PageData<RoleView> roles(int page, int pageSize, String search, String type) {
        return roles(TenantContextHolder.requireTenantId(), page, pageSize, search, type);
    }

    /** 从当前租户上下文取租户 ID 后查询角色详情。 */
    @Transactional(readOnly = true)
    public RoleView role(UUID id) {
        return role(TenantContextHolder.requireTenantId(), id);
    }

    /** 从当前租户上下文取租户 ID 后创建角色。 */
    @Transactional
    public RoleView createRole(RoleInput in) {
        return createRole(TenantContextHolder.requireTenantId(), in);
    }

    /** 从当前租户上下文取租户 ID 后更新角色。 */
    @Transactional
    public RoleView updateRole(UUID id, RoleInput in) {
        return updateRole(TenantContextHolder.requireTenantId(), id, in);
    }

    /** 从当前租户上下文取租户 ID 后删除角色。 */
    @Transactional
    public void deleteRole(UUID id) {
        deleteRole(TenantContextHolder.requireTenantId(), id);
    }

    /** 从当前租户上下文取租户 ID 后构建角色树。 */
    @Transactional(readOnly = true)
    public List<RoleTree> roleTree() {
        return roleTree(TenantContextHolder.requireTenantId());
    }

    /** 从当前租户上下文取租户 ID 后统计角色。 */
    @Transactional(readOnly = true)
    public RoleStats roleStats() {
        return roleStats(TenantContextHolder.requireTenantId());
    }

    /** 从当前租户上下文取租户 ID 后查询角色下的用户。 */
    @Transactional(readOnly = true)
    public RoleUsers roleUsers(UUID roleId, String search) {
        return roleUsers(TenantContextHolder.requireTenantId(), roleId, search);
    }

    /** 从当前租户上下文取租户 ID 后给角色分配用户。 */
    @Transactional
    public void assignUsers(UUID roleId, Collection<UUID> users) {
        assignUsers(TenantContextHolder.requireTenantId(), roleId, users);
    }

    /** 从当前租户上下文取租户 ID 后移除角色下的用户。 */
    @Transactional
    public void removeUsers(UUID roleId, Collection<UUID> users) {
        removeUsers(TenantContextHolder.requireTenantId(), roleId, users);
    }

    /** 从当前租户上下文取租户 ID 后查询用户的角色。 */
    @Transactional(readOnly = true)
    public List<RoleView> rolesForUser(UUID userId) {
        return rolesForUser(TenantContextHolder.requireTenantId(), userId);
    }

    /** 从当前租户上下文取租户 ID 后替换用户的角色分配。 */
    @Transactional
    public void replaceUserRoles(UUID userId, Collection<UUID> roleIds) {
        replaceUserRoles(TenantContextHolder.requireTenantId(), userId, roleIds);
    }

    /** 从当前租户上下文取租户 ID 后分页查询权限。 */
    @Transactional
    public PageData<PermissionView> permissions(int page, int pageSize, String search, String type, String resource) {
        return permissions(TenantContextHolder.requireTenantId(), page, pageSize, search, type, resource);
    }

    /** 从当前租户上下文取租户 ID 后查询权限详情。 */
    @Transactional(readOnly = true)
    public PermissionView permission(UUID id) {
        return permission(TenantContextHolder.requireTenantId(), id);
    }

    /** 从当前租户上下文取租户 ID 后创建权限。 */
    @Transactional
    public PermissionView createPermission(PermissionInput in) {
        return createPermission(TenantContextHolder.requireTenantId(), in);
    }

    /** 从当前租户上下文取租户 ID 后更新权限。 */
    @Transactional
    public PermissionView updatePermission(UUID id, PermissionInput in) {
        return updatePermission(TenantContextHolder.requireTenantId(), id, in);
    }

    /** 从当前租户上下文取租户 ID 后删除权限。 */
    @Transactional
    public void deletePermission(UUID id) {
        deletePermission(TenantContextHolder.requireTenantId(), id);
    }

    /** 从当前租户上下文取租户 ID 后构建权限树。 */
    @Transactional
    public List<PermissionTree> permissionTree() {
        return permissionTree(TenantContextHolder.requireTenantId());
    }

    /** 从当前租户上下文取租户 ID 后统计权限。 */
    @Transactional
    public PermissionStats permissionStats() {
        return permissionStats(TenantContextHolder.requireTenantId());
    }

    /**
     * 角色创建/更新入参。
     *
     * @param name        角色名称（必填）
     * @param code        角色编码（创建时必填，用于程序识别）
     * @param description 角色描述
     * @param type        角色类型：system（内置）/ custom（自定义）
     * @param permissions 权限集合（权限码 -> 是否启用）
     * @param dataScope   数据范围：all / department / department_and_sub / self
     * @param parentId    父角色 ID（可为 null，表示顶级角色）
     */
    

    /**
     * 角色统计视图。
     *
     * @param totalRoles  角色总数
     * @param systemRoles 内置（system）角色数
     * @param customRoles 自定义（custom）角色数
     * @param totalUsers  已分配角色的去重用户数
     */
    

    /**
     * 权限统计视图。
     *
     * @param totalPermissions  权限总数
     * @param menuPermissions   menu（菜单）类权限数
     * @param operationPermissions operation（操作）类权限数
     * @param dataPermissions   data（数据）类权限数
     */
    

    /**
     * 权限创建/更新入参。
     *
     * @param code        权限编码（创建时必填）
     * @param name        权限名称（必填）
     * @param description 权限描述
     * @param type        权限类型：menu / operation / data
     * @param parentId    父权限 ID（可为 null，表示顶级权限）
     * @param resource    权限对应的资源标识
     * @param action      权限对应的动作（如 read / create / delete）
     */
    

    /**
     * 角色详情视图。
     *
     * @param id          角色 ID
     * @param tenantId    所属租户 ID
     * @param name        角色名称
     * @param code        角色编码
     * @param description 角色描述
     * @param type        角色类型：system（内置）/ custom（自定义）
     * @param permissions 权限集合（权限码 -> 是否启用）
     * @param dataScope   数据范围：all / department / department_and_sub / self
     * @param parentId    父角色 ID
     * @param createdAt   创建时间
     * @param updatedAt   最后更新时间
     * @param userCount   分配该角色的用户数
     * @param parent      父角色摘要（可为 null）
     */
    

    /**
     * 角色下的用户视图。
     *
     * @param users 用户列表
     * @param total 用户总数
     */
    

    /**
     * 角色树节点视图。
     *
     * @param id          角色 ID
     * @param name        角色名称
     * @param code        角色编码
     * @param description 角色描述
     * @param type        角色类型
     * @param userCount   分配该角色的用户数
     * @param children    子角色节点列表
     */
    

    /**
     * 权限详情视图。
     *
     * @param id          权限 ID
     * @param tenantId    所属租户 ID
     * @param code        权限编码
     * @param name        权限名称
     * @param description 权限描述
     * @param type        权限类型：menu / operation / data
     * @param resource    权限对应的资源标识
     * @param action      权限对应的动作
     * @param parentId    父权限 ID
     * @param createdAt   创建时间
     * @param updatedAt   最后更新时间
     * @param parent      父权限摘要（可为 null）
     */
    

    /**
     * 父节点摘要视图。
     *
     * @param id   父节点 ID
     * @param name 父节点名称
     * @param code 父节点编码
     */
    

    /**
     * 权限树节点视图。
     *
     * @param id          权限 ID
     * @param code        权限编码
     * @param name        权限名称
     * @param description 权限描述
     * @param type        权限类型
     * @param resource    权限对应的资源标识
     * @param action      权限对应的动作
     * @param children    子权限节点列表
     */
    

    /** 角色树的可变构建节点（组装完成后转为不可变的 {@link RoleTree}）。 */
    private static final class MutableRoleTree {
        final RoleView role;
        final List<MutableRoleTree> children = new ArrayList<>();

        MutableRoleTree(RoleView r) {
            role = r;
        }

        RoleTree freeze() {
            return new RoleTree(role.id(), role.name(), role.code(), role.description(), role.type(), role.userCount(), children.stream().map(MutableRoleTree::freeze).toList());
        }
    }

    /** 权限树的可变构建节点（组装完成后转为不可变的 {@link PermissionTree}）。 */
    private static final class MutablePermissionTree {
        final PermissionView permission;
        final List<MutablePermissionTree> children = new ArrayList<>();

        MutablePermissionTree(PermissionView p) {
            permission = p;
        }

        PermissionTree freeze() {
            return new PermissionTree(permission.id(), permission.code(), permission.name(), permission.description(), permission.type(), permission.resource(), permission.action(), children.stream().map(MutablePermissionTree::freeze).toList());
        }
    }

    /**
     * 预置权限种子定义。
     *
     * @param code     权限编码
     * @param name     权限名称
     * @param type     权限类型：menu / operation / data
     * @param resource 资源标识
     * @param action   动作
     */
    
}
