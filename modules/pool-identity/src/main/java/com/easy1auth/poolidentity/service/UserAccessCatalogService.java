package com.easy1auth.poolidentity.service;

import com.easy1auth.poolidentity.dto.*;
import com.easy1auth.poolidentity.model.*;
import com.easy1auth.common.foundation.util.TenantContextHolder;

import com.easy1auth.poolidentity.dto.PoolUserView;
import com.easy1auth.common.foundation.error.DomainException;
import com.easy1auth.common.foundation.id.UuidV7;
import com.easy1auth.common.foundation.web.PageData;
import com.easy1auth.poolidentity.constant.ErrorCodeConstants;
import com.easy1auth.poolidentity.repository.UserAccessCatalogRepository;
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
    private final UserAccessCatalogRepository repository;
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

    public UserAccessCatalogService(UserAccessCatalogRepository repository, JdbcClient db) {
        this.repository = repository;
        this.db = db;
    }

    /** 分页查询租户下的角色，可按名称/编码模糊搜索、按类型过滤，按创建时间倒序。 */
    @Transactional(readOnly = true)
    public PageData<RoleView> roles(UUID tenant, int page, int pageSize, String search, String type) {
        int p = Math.max(1, page), size = Math.min(100, Math.max(1, pageSize));
        var rows = repository.findRoles(tenant, search, type);
        return PageData.of(rows.stream().skip((long) (p - 1) * size).limit(size).map(this::roleView).toList(), p, size, rows.size());
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
        repository.saveRole(e);
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
        repository.updateRole(tenant, id, name, scope, in.description(), in.permissions(), in.parentId());
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
        if (repository.hasRoleChildren(tenant, id)) {
            throw new DomainException(ErrorCodeConstants.ROLE_HAS_CHILDREN);
        }
        repository.deleteRole(tenant, id);
    }

    /** 构建租户下的角色树（按父角色 parentId 组织层级）。 */
    @Transactional(readOnly = true)
    public List<RoleTree> roleTree(UUID tenant) {
        var roles = repository.findAllRoles(tenant);
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
        var roles = repository.findAllRoles(tenant);
        var roleIds = roles.stream().map(PoolRoleEntity::id).toList();
        var assigned = roleIds.isEmpty() ? 0L : roleIds.stream().flatMap(roleId -> repository.findAssignedUserIds(tenant, roleId).stream()).distinct().count();
        return new RoleStats(roles.size(), roles.stream().filter(r -> "system".equals(r.type())).count(),
                roles.stream().filter(r -> "custom".equals(r.type())).count(), assigned);
    }

    /** 查询指定角色下的用户列表，可按用户名/邮箱/姓名模糊搜索。 */
    @Transactional(readOnly = true)
    public RoleUsers roleUsers(UUID tenant, UUID roleId, String search) {
        roleEntity(tenant, roleId);
        var userIds = repository.findAssignedUserIds(tenant, roleId);
        if (userIds.isEmpty()) {
            return new RoleUsers(List.of(), 0);
        }
        var users = repository.findUsers(tenant, userIds, search).stream().map(this::userView).toList();
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
        users.forEach(id -> repository.deleteAssignment(tenant, id, roleId));
    }

    /** 查询用户当前分配的全部角色。 */
    @Transactional(readOnly = true)
    public List<RoleView> rolesForUser(UUID tenant, UUID userId) {
        userEntity(tenant, userId);
        var ids = repository.findAssignedRoleIds(tenant, userId);
        return repository.findRolesByIds(tenant, ids).stream().map(this::roleView).toList();
    }

    /** 替换用户的全部角色分配：先删除旧的分配，再批量插入新的分配。 */
    @Transactional
    public void replaceUserRoles(UUID tenant, UUID userId, Collection<UUID> roleIds) {
        userEntity(tenant, userId);
        roleIds.forEach(id -> roleEntity(tenant, id));
        repository.deleteAssignmentsForUser(tenant, userId);
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
        var rows = repository.findPermissions(tenant, search, type, resource);
        return PageData.of(rows.stream().skip((long) (p - 1) * size).limit(size).map(this::permissionView).toList(), p, size, rows.size());
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
        repository.savePermission(e);
        return permissionView(e);
    }

    /** 更新权限的名称、类型、资源、动作、描述与父权限（仅更新传入的非空字段）。 */
    @Transactional
    public PermissionView updatePermission(UUID tenant, UUID id, PermissionInput in) {
        var old = permissionEntity(tenant, id);
        String name = in.name() == null ? old.name() : in.name(), type = in.type() == null ? old.type() : in.type(), resource = in.resource() == null ? old.resource() : in.resource(), action = in.action() == null ? old.action() : in.action();
        validatePermission(name, old.code(), type, resource, action);
        validatePermissionParent(tenant, id, in.parentId());
        repository.updatePermission(tenant, id, name, type, resource, action, in.description(), in.parentId());
        return permission(tenant, id);
    }

    /** 删除权限；存在子权限的权限不可删除。 */
    @Transactional
    public void deletePermission(UUID tenant, UUID id) {
        permissionEntity(tenant, id);
        if (repository.hasPermissionChildren(tenant, id)) {
            throw new DomainException(ErrorCodeConstants.PERMISSION_HAS_CHILDREN);
        }
        repository.deletePermission(tenant, id);
    }

    /** 构建租户下的权限树（按父权限 parentId 组织层级，首次访问自动初始化预置权限）。 */
    @Transactional
    public List<PermissionTree> permissionTree(UUID tenant) {
        ensurePresetPermissions(tenant);
        var rows = repository.findAllPermissions(tenant);
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
        var rows = repository.findPermissionTypes(tenant);
        return new PermissionStats(rows.size(), rows.stream().filter("menu"::equals).count(), rows.stream().filter("operation"::equals).count(), rows.stream().filter("data"::equals).count());
    }

    private void insertAssignment(UUID tenant, UUID user, UUID role) {
        var id = UserRoleAssignmentIdDraft.$.produce(d -> d.setTenantId(tenant).setUserId(user).setRoleId(role));
        repository.saveAssignment(UserRoleAssignmentEntityDraft.$.produce(d -> d.setId(id)));
    }

    /** 确保租户的预置权限已初始化：缺失的预置项通过原生 SQL 批量插入（幂等）。 */
    private void ensurePresetPermissions(UUID tenant) {
        var presetCodes = PRESETS.stream().map(PermissionSeed::code).toList();
        var existingCodes = new HashSet<>(repository.findPermissionCodes(tenant, presetCodes));
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
        return repository.findRole(tenant, id).orElseThrow(() -> new DomainException(ErrorCodeConstants.USER_ROLE_NOT_FOUND));
    }

    private PoolPermissionEntity permissionEntity(UUID tenant, UUID id) {
        return repository.findPermission(tenant, id).orElseThrow(() -> new DomainException(ErrorCodeConstants.USER_PERMISSION_NOT_FOUND));
    }

    private PoolUserEntity userEntity(UUID tenant, UUID id) {
        return repository.findUser(tenant, id).orElseThrow(() -> new DomainException(ErrorCodeConstants.USER_ACCESS_USER_NOT_FOUND));
    }

    private long assignmentCount(UUID tenant, UUID role) {
        return repository.assignmentCount(tenant, role);
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
        var p = repository.findRole(tenant, id).orElse(null);
        return p == null ? null : new ParentSummary(p.id(), p.name(), p.code());
    }

    private ParentSummary permissionParent(UUID tenant, UUID id) {
        if (id == null) {
            return null;
        }
        var p = repository.findPermission(tenant, id).orElse(null);
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

    
    
}
