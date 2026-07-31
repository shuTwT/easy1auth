package com.easy1auth.useraccess;

import com.easy1auth.tenant.TenantContextHolder;

import com.easy1auth.directory.PoolUserView;
import com.easy1auth.directory.model.PoolUserEntity;
import com.easy1auth.directory.model.PoolUserEntityTable;
import com.easy1auth.foundation.error.DomainException;
import com.easy1auth.foundation.id.UuidV7;
import com.easy1auth.useraccess.model.*;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class UserAccessCatalogService {
    private static final PoolRoleEntityTable ROLE = PoolRoleEntityTable.$;
    private static final PoolPermissionEntityTable PERMISSION = PoolPermissionEntityTable.$;
    private static final UserRoleAssignmentEntityTable ASSIGNMENT = UserRoleAssignmentEntityTable.$;
    private static final PoolUserEntityTable USER = PoolUserEntityTable.$;
    private final JSqlClient sql;
    private static final List<PermissionSeed> PRESETS = List.of(
            new PermissionSeed("user:read", "查看用户", "operation", "user", "read"), new PermissionSeed("user:create", "创建用户", "operation", "user", "create"), new PermissionSeed("user:update", "编辑用户", "operation", "user", "update"), new PermissionSeed("user:delete", "删除用户", "operation", "user", "delete"),
            new PermissionSeed("group:read", "查看用户组", "operation", "group", "read"), new PermissionSeed("group:create", "创建用户组", "operation", "group", "create"), new PermissionSeed("group:update", "编辑用户组", "operation", "group", "update"), new PermissionSeed("group:delete", "删除用户组", "operation", "group", "delete"),
            new PermissionSeed("position:read", "查看岗位", "operation", "position", "read"), new PermissionSeed("position:create", "创建岗位", "operation", "position", "create"), new PermissionSeed("position:update", "编辑岗位", "operation", "position", "update"), new PermissionSeed("position:delete", "删除岗位", "operation", "position", "delete"),
            new PermissionSeed("role:read", "查看角色", "operation", "role", "read"), new PermissionSeed("role:create", "创建角色", "operation", "role", "create"), new PermissionSeed("role:update", "编辑角色", "operation", "role", "update"), new PermissionSeed("role:delete", "删除角色", "operation", "role", "delete"), new PermissionSeed("role:assign", "分配角色", "operation", "role", "assign"),
            new PermissionSeed("permission:read", "查看权限", "operation", "permission", "read"), new PermissionSeed("permission:create", "创建权限", "operation", "permission", "create"), new PermissionSeed("permission:update", "编辑权限", "operation", "permission", "update"), new PermissionSeed("permission:delete", "删除权限", "operation", "permission", "delete"),
            new PermissionSeed("data:all", "全部数据", "data", "data", "all"), new PermissionSeed("data:department", "本部门数据", "data", "data", "department"), new PermissionSeed("data:department-sub", "本部门及下级数据", "data", "data", "department_and_sub"), new PermissionSeed("data:self", "仅本人数据", "data", "data", "self"),
            new PermissionSeed("menu:user", "用户管理", "menu", "menu", "user"), new PermissionSeed("menu:group", "用户组管理", "menu", "menu", "group"), new PermissionSeed("menu:position", "岗位管理", "menu", "menu", "position"), new PermissionSeed("menu:role", "角色管理", "menu", "menu", "role"), new PermissionSeed("menu:permission", "权限管理", "menu", "menu", "permission"));

    public UserAccessCatalogService(JSqlClient sql) {
        this.sql = sql;
    }

    @Transactional(readOnly = true)
    public RolePage roles(UUID tenant, int page, int pageSize, String search, String type) {
        int p = Math.max(1, page), size = Math.min(100, Math.max(1, pageSize));
        var query = sql.createQuery(ROLE).where(ROLE.tenantId().eq(tenant))
                .whereIf(search != null, () -> Predicate.or(ROLE.name().ilike(search, LikeMode.ANYWHERE), ROLE.code().ilike(search, LikeMode.ANYWHERE)))
                .whereIf(type != null, () -> ROLE.type().eq(type)).orderBy(ROLE.createdAt().desc()).select(ROLE);
        long total = query.fetchUnlimitedCount();
        return new RolePage(query.limit(size, (long) (p - 1) * size).execute().stream().map(this::roleView).toList(), total, p, size);
    }

    @Transactional(readOnly = true)
    public RoleView role(UUID tenant, UUID id) {
        return roleView(roleEntity(tenant, id));
    }

    @Transactional
    public RoleView createRole(UUID tenant, RoleInput in) {
        if ("system".equals(in.type())) {
            throw new DomainException("SYSTEM_ROLE_RESERVED", "系统角色只能由系统初始化", 403);
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

    @Transactional
    public RoleView updateRole(UUID tenant, UUID id, RoleInput in) {
        var old = roleEntity(tenant, id);
        if ("system".equals(old.type())) throw new DomainException("SYSTEM_ROLE_IMMUTABLE", "系统角色不能修改", 403);
        String name = in.name() == null ? old.name() : in.name(), scope = in.dataScope() == null ? old.dataScope() : in.dataScope();
        validateRole(name, old.code(), scope);
        validateRoleParent(tenant, id, in.parentId());
        var u = sql.createUpdate(ROLE).set(ROLE.name(), name).set(ROLE.dataScope(), scope).set(ROLE.updatedAt(), Instant.now()).where(ROLE.id().eq(id), ROLE.tenantId().eq(tenant));
        if (in.description() != null) u.set(ROLE.description(), in.description());
        if (in.permissions() != null) u.set(ROLE.permissions(), in.permissions());
        if (in.parentId() != null) u.set(ROLE.parentId(), in.parentId());
        u.execute();
        return role(tenant, id);
    }

    @Transactional
    public void deleteRole(UUID tenant, UUID id) {
        var role = roleEntity(tenant, id);
        if ("system".equals(role.type())) throw new DomainException("SYSTEM_ROLE_IMMUTABLE", "系统角色不能删除", 403);
        if (assignmentCount(tenant, id) > 0)
            throw new DomainException("ROLE_HAS_USERS", "角色下还有用户，不能删除", 409);
        if (sql.createQuery(ROLE).where(ROLE.tenantId().eq(tenant), ROLE.parentId().eq(id)).select(ROLE.id()).exists())
            throw new DomainException("ROLE_HAS_CHILDREN", "角色下还有子角色，不能删除", 409);
        sql.createDelete(ROLE).where(ROLE.id().eq(id), ROLE.tenantId().eq(tenant)).execute();
    }

    @Transactional(readOnly = true)
    public List<RoleTree> roleTree(UUID tenant) {
        var roles = sql.createQuery(ROLE).where(ROLE.tenantId().eq(tenant)).orderBy(ROLE.createdAt()).select(ROLE).execute();
        Map<UUID, MutableRoleTree> nodes = new LinkedHashMap<>();
        roles.forEach(r -> nodes.put(r.id(), new MutableRoleTree(roleView(r))));
        List<MutableRoleTree> roots = new ArrayList<>();
        nodes.values().forEach(n -> {
            var p = n.role.parentId() == null ? null : nodes.get(n.role.parentId());
            if (p == null) roots.add(n);
            else p.children.add(n);
        });
        return roots.stream().map(MutableRoleTree::freeze).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> roleStats(UUID tenant) {
        var roles = sql.createQuery(ROLE).where(ROLE.tenantId().eq(tenant)).select(ROLE).execute();
        var roleIds = roles.stream().map(PoolRoleEntity::id).toList();
        var assigned = roleIds.isEmpty() ? 0L : sql.createQuery(ASSIGNMENT).where(ASSIGNMENT.id().tenantId().eq(tenant), ASSIGNMENT.id().roleId().in(roleIds)).select(ASSIGNMENT.id().userId()).execute().stream().distinct().count();
        return Map.of("totalRoles", (long) roles.size(), "systemRoles", roles.stream().filter(r -> "system".equals(r.type())).count(),
                "customRoles", roles.stream().filter(r -> "custom".equals(r.type())).count(), "totalUsers", assigned);
    }

    @Transactional(readOnly = true)
    public RoleUsers roleUsers(UUID tenant, UUID roleId, String search) {
        roleEntity(tenant, roleId);
        var userIds = sql.createQuery(ASSIGNMENT).where(ASSIGNMENT.id().tenantId().eq(tenant), ASSIGNMENT.id().roleId().eq(roleId)).select(ASSIGNMENT.id().userId()).execute();
        if (userIds.isEmpty()) return new RoleUsers(List.of(), 0);
        var users = sql.createQuery(USER).where(USER.tenantId().eq(tenant), USER.id().in(userIds))
                .whereIf(search != null, () -> Predicate.or(USER.username().ilike(search, LikeMode.ANYWHERE), USER.email().ilike(search, LikeMode.ANYWHERE), USER.name().ilike(search, LikeMode.ANYWHERE)))
                .orderBy(USER.createdAt().desc()).select(USER).execute().stream().map(this::userView).toList();
        return new RoleUsers(users, users.size());
    }

    @Transactional
    public void assignUsers(UUID tenant, UUID roleId, Collection<UUID> users) {
        roleEntity(tenant, roleId);
        users.forEach(id -> userEntity(tenant, id));
        users.forEach(id -> insertAssignment(tenant, id, roleId));
    }

    @Transactional
    public void removeUsers(UUID tenant, UUID roleId, Collection<UUID> users) {
        roleEntity(tenant, roleId);
        users.forEach(id -> userEntity(tenant, id));
        users.forEach(id -> sql.createDelete(ASSIGNMENT).where(ASSIGNMENT.id().tenantId().eq(tenant), ASSIGNMENT.id().userId().eq(id), ASSIGNMENT.id().roleId().eq(roleId)).execute());
    }

    @Transactional(readOnly = true)
    public List<RoleView> rolesForUser(UUID tenant, UUID userId) {
        userEntity(tenant, userId);
        var ids = sql.createQuery(ASSIGNMENT).where(ASSIGNMENT.id().tenantId().eq(tenant), ASSIGNMENT.id().userId().eq(userId)).select(ASSIGNMENT.id().roleId()).execute();
        return ids.isEmpty() ? List.of() : sql.createQuery(ROLE).where(ROLE.tenantId().eq(tenant), ROLE.id().in(ids)).select(ROLE).execute().stream().map(this::roleView).toList();
    }

    @Transactional
    public void replaceUserRoles(UUID tenant, UUID userId, Collection<UUID> roleIds) {
        userEntity(tenant, userId);
        roleIds.forEach(id -> roleEntity(tenant, id));
        sql.createDelete(ASSIGNMENT).where(ASSIGNMENT.id().tenantId().eq(tenant), ASSIGNMENT.id().userId().eq(userId)).execute();
        roleIds.forEach(id -> insertAssignment(tenant, userId, id));
    }

    @Transactional(readOnly = true)
    public String effectiveDataScope(UUID tenant, UUID userId) {
        var scopes = rolesForUser(tenant, userId).stream().map(RoleView::dataScope).collect(java.util.stream.Collectors.toSet());
        if (scopes.contains("all")) return "all";
        if (scopes.contains("department_and_sub")) return "department_and_sub";
        if (scopes.contains("department")) return "department";
        return "self";
    }

    @Transactional
    public PermissionPage permissions(UUID tenant, int page, int pageSize, String search, String type, String resource) {
        ensurePresetPermissions(tenant);
        int p = Math.max(1, page), size = Math.min(200, Math.max(1, pageSize));
        var query = sql.createQuery(PERMISSION).where(PERMISSION.tenantId().eq(tenant))
                .whereIf(search != null, () -> Predicate.or(PERMISSION.name().ilike(search, LikeMode.ANYWHERE), PERMISSION.code().ilike(search, LikeMode.ANYWHERE), PERMISSION.resource().ilike(search, LikeMode.ANYWHERE)))
                .whereIf(type != null, () -> PERMISSION.type().eq(type)).whereIf(resource != null, () -> PERMISSION.resource().eq(resource))
                .orderBy(PERMISSION.resource(), PERMISSION.code()).select(PERMISSION);
        long total = query.fetchUnlimitedCount();
        return new PermissionPage(query.limit(size, (long) (p - 1) * size).execute().stream().map(this::permissionView).toList(), total, p, size);
    }

    @Transactional(readOnly = true)
    public PermissionView permission(UUID tenant, UUID id) {
        return permissionView(permissionEntity(tenant, id));
    }

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

    @Transactional
    public PermissionView updatePermission(UUID tenant, UUID id, PermissionInput in) {
        var old = permissionEntity(tenant, id);
        String name = in.name() == null ? old.name() : in.name(), type = in.type() == null ? old.type() : in.type(), resource = in.resource() == null ? old.resource() : in.resource(), action = in.action() == null ? old.action() : in.action();
        validatePermission(name, old.code(), type, resource, action);
        validatePermissionParent(tenant, id, in.parentId());
        var u = sql.createUpdate(PERMISSION).set(PERMISSION.name(), name).set(PERMISSION.type(), type).set(PERMISSION.resource(), resource).set(PERMISSION.action(), action).set(PERMISSION.updatedAt(), Instant.now()).where(PERMISSION.id().eq(id), PERMISSION.tenantId().eq(tenant));
        if (in.description() != null) u.set(PERMISSION.description(), in.description());
        if (in.parentId() != null) u.set(PERMISSION.parentId(), in.parentId());
        u.execute();
        return permission(tenant, id);
    }

    @Transactional
    public void deletePermission(UUID tenant, UUID id) {
        permissionEntity(tenant, id);
        if (sql.createQuery(PERMISSION).where(PERMISSION.tenantId().eq(tenant), PERMISSION.parentId().eq(id)).select(PERMISSION.id()).exists())
            throw new DomainException("PERMISSION_HAS_CHILDREN", "权限下还有子权限，不能删除", 409);
        sql.createDelete(PERMISSION).where(PERMISSION.id().eq(id), PERMISSION.tenantId().eq(tenant)).execute();
    }

    @Transactional
    public List<PermissionTree> permissionTree(UUID tenant) {
        ensurePresetPermissions(tenant);
        var rows = sql.createQuery(PERMISSION).where(PERMISSION.tenantId().eq(tenant)).orderBy(PERMISSION.type(), PERMISSION.resource(), PERMISSION.code()).select(PERMISSION).execute();
        Map<UUID, MutablePermissionTree> nodes = new LinkedHashMap<>();
        rows.forEach(x -> nodes.put(x.id(), new MutablePermissionTree(permissionView(x))));
        List<MutablePermissionTree> roots = new ArrayList<>();
        nodes.values().forEach(n -> {
            var p = n.permission.parentId() == null ? null : nodes.get(n.permission.parentId());
            if (p == null) roots.add(n);
            else p.children.add(n);
        });
        return roots.stream().map(MutablePermissionTree::freeze).toList();
    }

    @Transactional
    public Map<String, Long> permissionStats(UUID tenant) {
        ensurePresetPermissions(tenant);
        var rows = sql.createQuery(PERMISSION).where(PERMISSION.tenantId().eq(tenant)).select(PERMISSION.type()).execute();
        return Map.of("totalPermissions", (long) rows.size(), "menuPermissions", rows.stream().filter("menu"::equals).count(), "operationPermissions", rows.stream().filter("operation"::equals).count(), "dataPermissions", rows.stream().filter("data"::equals).count());
    }

    private void insertAssignment(UUID tenant, UUID user, UUID role) {
        var id = UserRoleAssignmentIdDraft.$.produce(d -> d.setTenantId(tenant).setUserId(user).setRoleId(role));
        sql.saveCommand(UserRoleAssignmentEntityDraft.$.produce(d -> d.setId(id))).setMode(SaveMode.INSERT_IF_ABSENT).execute();
    }

    private void ensurePresetPermissions(UUID tenant) {
        Instant now = Instant.now();
        var entities = PRESETS.stream().map(p -> PoolPermissionEntityDraft.$.produce(d -> d.setId(UuidV7.randomUuid()).setTenantId(tenant).setCode(p.code()).setName(p.name()).setDescription(null).setType(p.type()).setParentId(null).setResource(p.resource()).setAction(p.action()).setCreatedAt(now).setUpdatedAt(now))).toList();
        sql.saveEntitiesCommand(entities)
                .setMode(SaveMode.INSERT_IF_ABSENT)
                .setKeyProps(PoolPermissionEntityProps.TENANT_ID, PoolPermissionEntityProps.CODE)
                .execute();
    }

    private PoolRoleEntity roleEntity(UUID tenant, UUID id) {
        return sql.createQuery(ROLE).where(ROLE.id().eq(id), ROLE.tenantId().eq(tenant)).select(ROLE).fetchOptional().orElseThrow(() -> missing("角色"));
    }

    private PoolPermissionEntity permissionEntity(UUID tenant, UUID id) {
        return sql.createQuery(PERMISSION).where(PERMISSION.id().eq(id), PERMISSION.tenantId().eq(tenant)).select(PERMISSION).fetchOptional().orElseThrow(() -> missing("权限"));
    }

    private PoolUserEntity userEntity(UUID tenant, UUID id) {
        return sql.createQuery(USER).where(USER.id().eq(id), USER.tenantId().eq(tenant)).select(USER).fetchOptional().orElseThrow(() -> missing("用户"));
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
        if (id == null) return null;
        var p = sql.createQuery(ROLE).where(ROLE.id().eq(id), ROLE.tenantId().eq(tenant)).select(ROLE).fetchOneOrNull();
        return p == null ? null : new ParentSummary(p.id(), p.name(), p.code());
    }

    private ParentSummary permissionParent(UUID tenant, UUID id) {
        if (id == null) return null;
        var p = sql.createQuery(PERMISSION).where(PERMISSION.id().eq(id), PERMISSION.tenantId().eq(tenant)).select(PERMISSION).fetchOneOrNull();
        return p == null ? null : new ParentSummary(p.id(), p.name(), p.code());
    }

    private PoolUserView userView(PoolUserEntity e) {
        return new PoolUserView(e.id(), e.tenantId(), e.username(), e.email(), e.phone(), e.name(), e.avatar(), e.status(), e.emailVerified(), e.phoneVerified(), e.department(), e.position(), e.customAttributes(), e.lastLoginAt(), e.createdAt(), e.updatedAt());
    }

    private void validateRoleParent(UUID tenant, UUID self, UUID parent) {
        if (parent == null) return;
        if (parent.equals(self)) throw new DomainException("ROLE_PARENT_SELF", "不能将自身设为父角色", 400);
        var p = roleEntity(tenant, parent);
        Set<UUID> seen = new HashSet<>();
        while (p.parentId() != null) {
            if (!seen.add(p.id()) || p.parentId().equals(self))
                throw new DomainException("ROLE_CYCLE", "角色层级不能形成循环", 400);
            p = roleEntity(tenant, p.parentId());
        }
    }

    private void validatePermissionParent(UUID tenant, UUID self, UUID parent) {
        if (parent == null) return;
        if (parent.equals(self)) throw new DomainException("PERMISSION_PARENT_SELF", "不能将自身设为父权限", 400);
        var p = permissionEntity(tenant, parent);
        Set<UUID> seen = new HashSet<>();
        while (p.parentId() != null) {
            if (!seen.add(p.id()) || p.parentId().equals(self))
                throw new DomainException("PERMISSION_CYCLE", "权限层级不能形成循环", 400);
            p = permissionEntity(tenant, p.parentId());
        }
    }

    private void validateRole(String name, String code, String scope) {
        if (name == null || name.isBlank() || code == null || code.isBlank() || !Set.of("all", "department", "department_and_sub", "self").contains(scope == null ? "self" : scope))
            throw new DomainException("ROLE_INVALID", "角色字段或数据范围无效", 400);
    }

    private void validatePermission(String name, String code, String type, String resource, String action) {
        if (name == null || code == null || resource == null || action == null || !Set.of("menu", "operation", "data").contains(type == null ? "operation" : type))
            throw new DomainException("PERMISSION_INVALID", "权限字段无效", 400);
    }

    private DomainException missing(String type) {
        return new DomainException("USER_ACCESS_NOT_FOUND", type + "不存在", 404);
    }

    @Transactional(readOnly = true)
    public RolePage roles(int page, int pageSize, String search, String type) {
        return roles(TenantContextHolder.requireTenantId(), page, pageSize, search, type);
    }

    @Transactional(readOnly = true)
    public RoleView role(UUID id) {
        return role(TenantContextHolder.requireTenantId(), id);
    }

    @Transactional
    public RoleView createRole(RoleInput in) {
        return createRole(TenantContextHolder.requireTenantId(), in);
    }

    @Transactional
    public RoleView updateRole(UUID id, RoleInput in) {
        return updateRole(TenantContextHolder.requireTenantId(), id, in);
    }

    @Transactional
    public void deleteRole(UUID id) {
        deleteRole(TenantContextHolder.requireTenantId(), id);
    }

    @Transactional(readOnly = true)
    public List<RoleTree> roleTree() {
        return roleTree(TenantContextHolder.requireTenantId());
    }

    @Transactional(readOnly = true)
    public Map<String, Long> roleStats() {
        return roleStats(TenantContextHolder.requireTenantId());
    }

    @Transactional(readOnly = true)
    public RoleUsers roleUsers(UUID roleId, String search) {
        return roleUsers(TenantContextHolder.requireTenantId(), roleId, search);
    }

    @Transactional
    public void assignUsers(UUID roleId, Collection<UUID> users) {
        assignUsers(TenantContextHolder.requireTenantId(), roleId, users);
    }

    @Transactional
    public void removeUsers(UUID roleId, Collection<UUID> users) {
        removeUsers(TenantContextHolder.requireTenantId(), roleId, users);
    }

    @Transactional(readOnly = true)
    public List<RoleView> rolesForUser(UUID userId) {
        return rolesForUser(TenantContextHolder.requireTenantId(), userId);
    }

    @Transactional
    public void replaceUserRoles(UUID userId, Collection<UUID> roleIds) {
        replaceUserRoles(TenantContextHolder.requireTenantId(), userId, roleIds);
    }

    @Transactional(readOnly = true)
    public PermissionPage permissions(int page, int pageSize, String search, String type, String resource) {
        return permissions(TenantContextHolder.requireTenantId(), page, pageSize, search, type, resource);
    }

    @Transactional(readOnly = true)
    public PermissionView permission(UUID id) {
        return permission(TenantContextHolder.requireTenantId(), id);
    }

    @Transactional
    public PermissionView createPermission(PermissionInput in) {
        return createPermission(TenantContextHolder.requireTenantId(), in);
    }

    @Transactional
    public PermissionView updatePermission(UUID id, PermissionInput in) {
        return updatePermission(TenantContextHolder.requireTenantId(), id, in);
    }

    @Transactional
    public void deletePermission(UUID id) {
        deletePermission(TenantContextHolder.requireTenantId(), id);
    }

    @Transactional(readOnly = true)
    public List<PermissionTree> permissionTree() {
        return permissionTree(TenantContextHolder.requireTenantId());
    }

    @Transactional(readOnly = true)
    public Map<String, Long> permissionStats() {
        return permissionStats(TenantContextHolder.requireTenantId());
    }

    public record RoleInput(String name, String code, String description, String type, Map<String, Boolean> permissions,
                            String dataScope, UUID parentId) {
    }

    public record PermissionInput(String code, String name, String description, String type, UUID parentId,
                                  String resource, String action) {
    }

    public record RoleView(UUID id, UUID tenantId, String name, String code, String description, String type,
                           Map<String, Boolean> permissions, String dataScope, UUID parentId, Instant createdAt,
                           Instant updatedAt, long userCount, ParentSummary parent) {
    }

    public record RolePage(List<RoleView> roles, long total, int page, int pageSize) {
    }

    public record RoleUsers(List<PoolUserView> users, int total) {
    }

    public record RoleTree(UUID id, String name, String code, String description, String type, long userCount,
                           List<RoleTree> children) {
    }

    public record PermissionView(UUID id, UUID tenantId, String code, String name, String description, String type,
                                 String resource, String action, UUID parentId, Instant createdAt, Instant updatedAt,
                                 ParentSummary parent) {
    }

    public record ParentSummary(UUID id, String name, String code) {
    }

    public record PermissionPage(List<PermissionView> permissions, long total, int page, int pageSize) {
    }

    public record PermissionTree(UUID id, String code, String name, String description, String type, String resource,
                                 String action, List<PermissionTree> children) {
    }

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

    private record PermissionSeed(String code, String name, String type, String resource, String action) {
    }
}
