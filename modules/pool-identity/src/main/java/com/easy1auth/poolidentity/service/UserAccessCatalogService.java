package com.easy1auth.poolidentity.service;

import com.easy1auth.poolidentity.dto.*;
import com.easy1auth.poolidentity.model.*;
import com.easy1auth.framework.tenant.context.TenantContextHolder;

import com.easy1auth.poolidentity.dto.PoolUserView;
import com.easy1auth.framework.common.error.DomainException;
import com.easy1auth.framework.common.id.UuidV7;
import com.easy1auth.framework.common.pagination.PageData;
import com.easy1auth.poolidentity.constant.ErrorCodeConstants;
import com.easy1auth.poolidentity.repository.UserAccessCatalogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * 用户访问控制目录服务：管理 pool_user 的角色、权限及其层级与分配关系。
 *
 * <p>角色（{@link PoolRoleEntity}）按租户隔离，支持数据范围（dataScope）与
 * 权限集合配置，内置角色（type=system）不可修改或删除；权限（
 * {@link PoolPermissionEntity}）区分 menu / operation / data 三类，可组成树形结构。
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
    public UserAccessCatalogService(UserAccessCatalogRepository repository) {
        this.repository = repository;
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

    /** 分页查询租户下的权限，支持名称/编码/资源搜索及类型/资源过滤。 */
    @Transactional
    public PageData<PermissionView> permissions(UUID tenant, int page, int pageSize, String search, String type, String resource) {
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
                .setDescription(in.description()).setType("data").setParentId(in.parentId()).setSpaceId(in.spaceId()).setOperations(in.operations() == null ? List.of() : in.operations()).setResource(in.resource()).setAction(in.action()).setCreatedAt(now).setUpdatedAt(now));
        repository.savePermission(e);
        return permissionView(e);
    }

    /** 更新权限的名称、类型、资源、动作、描述与父权限（仅更新传入的非空字段）。 */
    @Transactional
    public PermissionView updatePermission(UUID tenant, UUID id, PermissionInput in) {
        var old = permissionEntity(tenant, id);
        String name = in.name() == null ? old.name() : in.name(), type = "data", resource = in.resource() == null ? old.resource() : in.resource(), action = in.action() == null ? old.action() : in.action();
        validatePermission(name, old.code(), type, resource, action);
        validatePermissionParent(tenant, id, in.parentId());
        repository.updatePermission(tenant, id, name, type, resource, action, in.description(), in.parentId(), in.spaceId(), in.operations());
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

    /** 构建租户下的权限树（按父权限 parentId 组织层级）。 */
    @Transactional
    public List<PermissionTree> permissionTree(UUID tenant) {
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
        var rows = repository.findPermissionTypes(tenant);
        return new PermissionStats(rows.size(), rows.stream().filter("menu"::equals).count(), rows.stream().filter("operation"::equals).count(), rows.stream().filter("data"::equals).count());
    }

    @Transactional(readOnly = true)
    public List<PermissionSpaceView> permissionSpaces(UUID tenant, String search) {
        return repository.findPermissionSpaces(tenant, search).stream().map(this::permissionSpaceView).toList();
    }

    @Transactional
    public PermissionSpaceView createPermissionSpace(UUID tenant, PermissionSpaceInput in) {
        if (in.name() == null || in.name().isBlank() || in.code() == null || in.code().isBlank()) {
            throw new DomainException(ErrorCodeConstants.PERMISSION_INVALID);
        }
        Instant now = Instant.now();
        var entity = PoolPermissionSpaceEntityDraft.$.produce(d -> d.setId(UuidV7.randomUuid()).setTenantId(tenant)
                .setName(in.name()).setCode(in.code()).setDescription(in.description()).setCreatedAt(now).setUpdatedAt(now));
        repository.savePermissionSpace(entity);
        return permissionSpaceView(entity);
    }

    @Transactional
    public PermissionSpaceView updatePermissionSpace(UUID tenant, UUID id, PermissionSpaceInput in) {
        var old = repository.findPermissionSpace(tenant, id).orElseThrow(() -> new DomainException(ErrorCodeConstants.USER_PERMISSION_NOT_FOUND));
        repository.updatePermissionSpace(tenant, id, in.name() == null ? old.name() : in.name(), in.description());
        return permissionSpaceView(repository.findPermissionSpace(tenant, id).orElseThrow());
    }

    @Transactional
    public void deletePermissionSpace(UUID tenant, UUID id) {
        repository.findPermissionSpace(tenant, id).orElseThrow(() -> new DomainException(ErrorCodeConstants.USER_PERMISSION_NOT_FOUND));
        repository.deletePermissionSpace(tenant, id);
    }

    private PermissionSpaceView permissionSpaceView(PoolPermissionSpaceEntity space) {
        return new PermissionSpaceView(space.id(), space.tenantId(), space.name(), space.code(), space.description(), space.createdAt(), space.updatedAt());
    }

    private void insertAssignment(UUID tenant, UUID user, UUID role) {
        var id = UserRoleAssignmentIdDraft.$.produce(d -> d.setTenantId(tenant).setUserId(user).setRoleId(role));
        repository.saveAssignment(UserRoleAssignmentEntityDraft.$.produce(d -> d.setId(id)));
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
        return new PermissionView(p.id(), p.tenantId(), p.code(), p.name(), p.description(), p.type(), p.resource(), p.action(), p.parentId(), p.spaceId(), p.operations(), p.createdAt(), p.updatedAt(), permissionParent(p.tenantId(), p.parentId()));
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
            return new PermissionTree(permission.id(), permission.code(), permission.name(), permission.description(), permission.type(), permission.resource(), permission.action(), permission.spaceId(), permission.operations(), permission.parent(), children.stream().map(MutablePermissionTree::freeze).toList());
        }
    }

    
    
}
