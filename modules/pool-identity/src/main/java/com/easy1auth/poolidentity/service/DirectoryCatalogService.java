package com.easy1auth.poolidentity.service;

import com.easy1auth.poolidentity.constant.ErrorCodeConstants;
import com.easy1auth.poolidentity.dto.PoolUserView;
import com.easy1auth.poolidentity.dto.*;
import com.easy1auth.poolidentity.model.*;
import com.easy1auth.tenant.util.TenantContextHolder;

import com.easy1auth.infrastructure.foundation.error.DomainException;
import com.easy1auth.infrastructure.foundation.id.UuidV7;
import com.easy1auth.infrastructure.foundation.web.PageData;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

/**
 * 目录目录服务：管理 pool_user 的用户组（Group）、岗位（Position）及其组织关系。
 *
 * <p>用户组支持树形层级与多种类型（team / department / project / organization），
 * 可维护组成员与组管理员；岗位记录编码、层级与部门归属。用户组、岗位均按租户隔离，
 * 服务于 pool_user 的组织架构管理。</p>
 */
@Service
public class DirectoryCatalogService {
    /** 用户组表（user_group）静态描述符 */
    private static final UserGroupEntityTable GROUP = UserGroupEntityTable.$;
    /** 岗位表（position）静态描述符 */
    private static final PositionEntityTable POSITION = PositionEntityTable.$;
    /** pool_user 用户表静态描述符 */
    private static final PoolUserEntityTable USER = PoolUserEntityTable.$;
    /** 用户-组成员分配表（pool_user_group）静态描述符 */
    private static final UserGroupAssignmentEntityTable MEMBER = UserGroupAssignmentEntityTable.$;
    /** 用户-组管理员分配表（pool_group_admin）静态描述符 */
    private static final GroupAdminAssignmentEntityTable ADMIN = GroupAdminAssignmentEntityTable.$;
    /** jimmer SQL 客户端 */
    private final JSqlClient sql;

    public DirectoryCatalogService(JSqlClient sql) {
        this.sql = sql;
    }

    /** 分页查询租户下的用户组，可按名称模糊搜索、按类型/父组过滤，按创建时间倒序。 */
    @Transactional(readOnly = true)
    public PageData<GroupView> groups(UUID tenant, int page, int size, String name, String type, UUID parentId) {
        int p = Math.max(1, page), s = Math.min(100, Math.max(1, size));
        var query = sql.createQuery(GROUP).where(GROUP.tenantId().eq(tenant))
                .whereIf(name != null, () -> GROUP.name().ilike(name, LikeMode.ANYWHERE))
                .whereIf(type != null, () -> GROUP.type().eq(type))
                .whereIf(parentId != null, () -> GROUP.parentId().eq(parentId))
                .orderBy(GROUP.createdAt().desc()).select(GROUP);
        long total = query.fetchUnlimitedCount();
        return PageData.of(query.limit(s, (long) (p - 1) * s).execute().stream().map(this::groupView).toList(), p, s, total);
    }

    /** 查询租户下单个用户组的详情视图。 */
    @Transactional(readOnly = true)
    public GroupView group(UUID tenant, UUID id) {
        return groupView(groupEntity(tenant, id));
    }

    /** 创建用户组（默认类型 team）：校验名称/类型与父组合法性。 */
    @Transactional
    public GroupView createGroup(UUID tenant, GroupInput in) {
        validateGroup(in.name(), in.type());
        validateParent(tenant, null, in.parentId());
        Instant now = Instant.now();
        var entity = UserGroupEntityDraft.$.produce(d -> d.setId(UuidV7.randomUuid()).setTenantId(tenant)
                .setName(in.name().strip()).setDescription(in.description()).setType(in.type() == null ? "team" : in.type())
                .setParentId(in.parentId()).setCreatedAt(now).setUpdatedAt(now));
        sql.saveCommand(entity).setMode(SaveMode.INSERT_ONLY).execute();
        return groupView(entity);
    }

    /** 更新用户组的名称、类型、描述与父组（仅更新传入的非空字段）；企业身份源托管的组不可修改。 */
    @Transactional
    public GroupView updateGroup(UUID tenant, UUID id, GroupInput in) {
        var old = groupEntity(tenant, id);
        rejectEnterpriseManaged(old);
        String name = in.name() == null ? old.name() : in.name();
        String type = in.type() == null ? old.type() : in.type();
        validateGroup(name, type);
        validateParent(tenant, id, in.parentId());
        var update = sql.createUpdate(GROUP).set(GROUP.name(), name).set(GROUP.type(), type).set(GROUP.updatedAt(), Instant.now())
                .where(GROUP.id().eq(id), GROUP.tenantId().eq(tenant));
        if (in.description() != null) {
            update.set(GROUP.description(), in.description());
        }
        if (in.parentId() != null) {
            update.set(GROUP.parentId(), in.parentId());
        }
        update.execute();
        return group(tenant, id);
    }

    /** 删除用户组；存在子组的用户组不可删除，企业身份源托管的组不可删除。 */
    @Transactional
    public void deleteGroup(UUID tenant, UUID id) {
        rejectEnterpriseManaged(groupEntity(tenant, id));
        if (sql.createQuery(GROUP).where(GROUP.tenantId().eq(tenant), GROUP.parentId().eq(id)).select(GROUP.id()).exists()) {
            throw new DomainException(ErrorCodeConstants.GROUP_HAS_CHILDREN);
        }
        sql.createDelete(GROUP).where(GROUP.id().eq(id), GROUP.tenantId().eq(tenant)).execute();
    }

    /** 构建租户下的用户组树（按父组 parentId 组织层级，含成员/管理员/子组数量）。 */
    @Transactional(readOnly = true)
    public List<GroupTree> groupTree(UUID tenant) {
        var groups = sql.createQuery(GROUP).where(GROUP.tenantId().eq(tenant)).orderBy(GROUP.createdAt()).select(GROUP).execute();
        Map<UUID, MutableGroupTree> nodes = new LinkedHashMap<>();
        groups.forEach(g -> nodes.put(g.id(), new MutableGroupTree(g, memberCount(g.tenantId(), g.id()), adminCount(g.tenantId(), g.id()))));
        List<MutableGroupTree> roots = new ArrayList<>();
        nodes.values().forEach(n -> {
            var parent = n.entity.parentId() == null ? null : nodes.get(n.entity.parentId());
            if (parent == null) {
                roots.add(n);
            } else {
                parent.children.add(n);
            }
        });
        return roots.stream().map(MutableGroupTree::freeze).toList();
    }

    /** 统计租户下的用户组数量（按类型拆分，含顶级组数量）。 */
    @Transactional(readOnly = true)
    public GroupStats groupStats(UUID tenant) {
        var rows = sql.createQuery(GROUP).where(GROUP.tenantId().eq(tenant)).select(GROUP).execute();
        return new GroupStats(rows.size(), countType(rows, "team"), countType(rows, "department"), countType(rows, "project"),
                countType(rows, "organization"), rows.stream().filter(g -> g.parentId() == null).count());
    }

    /** 查询用户组的成员与管理员列表（含成员总数）。 */
    @Transactional(readOnly = true)
    public GroupMembers groupMembers(UUID tenant, UUID groupId) {
        groupEntity(tenant, groupId);
        var memberIds = sql.createQuery(MEMBER).where(MEMBER.id().tenantId().eq(tenant), MEMBER.id().groupId().eq(groupId)).select(MEMBER.id().userId()).execute();
        var adminIds = sql.createQuery(ADMIN).where(ADMIN.id().tenantId().eq(tenant), ADMIN.id().groupId().eq(groupId)).select(ADMIN.id().userId()).execute();
        return new GroupMembers(userViews(tenant, memberIds), userViews(tenant, adminIds), memberIds.size());
    }

    /** 批量向用户组添加成员（已存在的分配自动跳过）。 */
    @Transactional
    public void addMembers(UUID tenant, UUID groupId, Collection<UUID> users) {
        mutateGroupUsers(tenant, groupId, users, false, true);
    }

    /** 批量从用户组移除成员。 */
    @Transactional
    public void removeMembers(UUID tenant, UUID groupId, Collection<UUID> users) {
        mutateGroupUsers(tenant, groupId, users, false, false);
    }

    /** 批量向用户组添加管理员（已存在的分配自动跳过）。 */
    @Transactional
    public void addAdmins(UUID tenant, UUID groupId, Collection<UUID> users) {
        mutateGroupUsers(tenant, groupId, users, true, true);
    }

    /** 批量移除用户组的组管理员。 */
    @Transactional
    public void removeAdmins(UUID tenant, UUID groupId, Collection<UUID> users) {
        mutateGroupUsers(tenant, groupId, users, true, false);
    }

    /** 分页查询租户下的岗位，可按名称/编码模糊搜索、按部门/层级过滤，按创建时间倒序。 */
    @Transactional(readOnly = true)
    public PageData<PositionView> positions(UUID tenant, int page, int size, String name, String code, UUID departmentId, Integer level) {
        int p = Math.max(1, page), s = Math.min(100, Math.max(1, size));
        var query = sql.createQuery(POSITION).where(POSITION.tenantId().eq(tenant))
                .whereIf(name != null, () -> POSITION.name().ilike(name, LikeMode.ANYWHERE))
                .whereIf(code != null, () -> POSITION.code().ilike(code, LikeMode.ANYWHERE))
                .whereIf(departmentId != null, () -> POSITION.departmentId().eq(departmentId))
                .whereIf(level != null, () -> POSITION.level().eq(level)).orderBy(POSITION.createdAt().desc()).select(POSITION);
        long total = query.fetchUnlimitedCount();
        return PageData.of(query.limit(s, (long) (p - 1) * s).execute().stream().map(this::positionView).toList(), p, s, total);
    }

    /** 查询租户下单个岗位的详情视图。 */
    @Transactional(readOnly = true)
    public PositionView position(UUID tenant, UUID id) {
        return positionView(positionEntity(tenant, id));
    }

    /** 创建岗位：校验名称与编码，默认层级 level=1。 */
    @Transactional
    public PositionView createPosition(UUID tenant, PositionInput in) {
        validatePosition(in.name(), in.code());
        Instant now = Instant.now();
        var e = PositionEntityDraft.$.produce(d -> d.setId(UuidV7.randomUuid()).setTenantId(tenant).setName(in.name()).setCode(in.code())
                .setDescription(in.description()).setDepartmentId(in.departmentId()).setLevel(in.level() == null ? 1 : in.level())
                .setSequence(in.sequence()).setMaxCount(in.maxCount()).setCreatedAt(now).setUpdatedAt(now));
        sql.saveCommand(e).setMode(SaveMode.INSERT_ONLY).execute();
        return positionView(e);
    }

    /** 更新岗位的名称、编码、描述、部门、层级、排序与人数上限（仅更新传入的非空字段）。 */
    @Transactional
    public PositionView updatePosition(UUID tenant, UUID id, PositionInput in) {
        var old = positionEntity(tenant, id);
        String name = in.name() == null ? old.name() : in.name();
        String code = in.code() == null ? old.code() : in.code();
        validatePosition(name, code);
        var u = sql.createUpdate(POSITION).set(POSITION.name(), name).set(POSITION.code(), code).set(POSITION.updatedAt(), Instant.now()).where(POSITION.id().eq(id), POSITION.tenantId().eq(tenant));
        if (in.description() != null) {
            u.set(POSITION.description(), in.description());
        }
        if (in.departmentId() != null) {
            u.set(POSITION.departmentId(), in.departmentId());
        }
        if (in.level() != null) {
            u.set(POSITION.level(), in.level());
        }
        if (in.sequence() != null) {
            u.set(POSITION.sequence(), in.sequence());
        }
        if (in.maxCount() != null) {
            u.set(POSITION.maxCount(), in.maxCount());
        }
        u.execute();
        return position(tenant, id);
    }

    /** 删除岗位。 */
    @Transactional
    public void deletePosition(UUID tenant, UUID id) {
        positionEntity(tenant, id);
        sql.createDelete(POSITION).where(POSITION.id().eq(id), POSITION.tenantId().eq(tenant)).execute();
    }

    /** 统计租户下的岗位数量：总数、在编（有用户）/空编数量及平均层级。 */
    @Transactional(readOnly = true)
    public PositionStats positionStats(UUID tenant) {
        var rows = sql.createQuery(POSITION).where(POSITION.tenantId().eq(tenant)).select(POSITION).execute();
        long filled = rows.stream().filter(p -> positionUserCount(p) > 0).count();
        return new PositionStats(rows.size(), filled, rows.size() - filled,
                rows.stream().mapToInt(PositionEntity::level).average().orElse(0));
    }

    /** 查询用户当前加入的全部用户组。 */
    @Transactional(readOnly = true)
    public List<GroupView> groupsForUser(UUID tenant, UUID userId) {
        userEntity(tenant, userId);
        var ids = sql.createQuery(MEMBER).where(MEMBER.id().tenantId().eq(tenant), MEMBER.id().userId().eq(userId)).select(MEMBER.id().groupId()).execute();
        return ids.isEmpty() ? List.of() : sql.createQuery(GROUP).where(GROUP.tenantId().eq(tenant), GROUP.id().in(ids)).select(GROUP).execute().stream().map(this::groupView).toList();
    }

    /** 替换用户加入的用户组：先删除旧的成员分配，再批量插入新的分配。 */
    @Transactional
    public void replaceUserGroups(UUID tenant, UUID userId, Collection<UUID> groupIds) {
        userEntity(tenant, userId);
        for (UUID groupId : groupIds) {
            groupEntity(tenant, groupId);
        }
        sql.createDelete(MEMBER).where(MEMBER.id().tenantId().eq(tenant), MEMBER.id().userId().eq(userId)).execute();
        groupIds.forEach(groupId -> insertMember(tenant, userId, groupId));
    }

    private void mutateGroupUsers(UUID tenant, UUID groupId, Collection<UUID> userIds, boolean admins, boolean add) {
        rejectEnterpriseManaged(groupEntity(tenant, groupId));
        userIds.forEach(id -> userEntity(tenant, id));
        for (UUID userId : userIds) {
            if (admins) {
                if (add) {
                    insertAdmin(tenant, groupId, userId);
                } else {
                    sql.createDelete(ADMIN).where(ADMIN.id().tenantId().eq(tenant), ADMIN.id().groupId().eq(groupId), ADMIN.id().userId().eq(userId)).execute();
                }
            } else {
                if (add) {
                    insertMember(tenant, userId, groupId);
                } else {
                    sql.createDelete(MEMBER).where(MEMBER.id().tenantId().eq(tenant), MEMBER.id().groupId().eq(groupId), MEMBER.id().userId().eq(userId)).execute();
                }
            }
        }
    }

    private void insertMember(UUID tenant, UUID user, UUID group) {
        var id = UserGroupAssignmentIdDraft.$.produce(d -> d.setTenantId(tenant).setUserId(user).setGroupId(group));
        sql.saveCommand(UserGroupAssignmentEntityDraft.$.produce(d -> d.setId(id))).setMode(SaveMode.INSERT_IF_ABSENT).execute();
    }

    private void insertAdmin(UUID tenant, UUID group, UUID user) {
        var id = GroupAdminAssignmentIdDraft.$.produce(d -> d.setTenantId(tenant).setGroupId(group).setUserId(user));
        sql.saveCommand(GroupAdminAssignmentEntityDraft.$.produce(d -> d.setId(id))).setMode(SaveMode.INSERT_IF_ABSENT).execute();
    }

    private UserGroupEntity groupEntity(UUID tenant, UUID id) {
        return sql.createQuery(GROUP).where(GROUP.id().eq(id), GROUP.tenantId().eq(tenant)).select(GROUP).fetchOptional().orElseThrow(() -> new DomainException(ErrorCodeConstants.DIRECTORY_GROUP_NOT_FOUND));
    }

    private PositionEntity positionEntity(UUID tenant, UUID id) {
        return sql.createQuery(POSITION).where(POSITION.id().eq(id), POSITION.tenantId().eq(tenant)).select(POSITION).fetchOptional().orElseThrow(() -> new DomainException(ErrorCodeConstants.DIRECTORY_POSITION_NOT_FOUND));
    }

    private PoolUserEntity userEntity(UUID tenant, UUID id) {
        return sql.createQuery(USER).where(USER.id().eq(id), USER.tenantId().eq(tenant)).select(USER).fetchOptional().orElseThrow(() -> new DomainException(ErrorCodeConstants.DIRECTORY_USER_NOT_FOUND));
    }

    private List<PoolUserView> userViews(UUID tenant, Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return sql.createQuery(USER).where(USER.tenantId().eq(tenant), USER.id().in(ids)).select(USER).execute().stream().map(this::userView).toList();
    }

    private GroupView groupView(UserGroupEntity g) {
        return new GroupView(g.id(), g.tenantId(), g.name(), g.description(), g.type(), g.parentId(), g.createdAt(), g.updatedAt(), new Counts(memberCount(g.tenantId(), g.id()), adminCount(g.tenantId(), g.id()), childCount(g.tenantId(), g.id())));
    }

    private PositionView positionView(PositionEntity p) {
        return new PositionView(p.id(), p.tenantId(), p.name(), p.code(), p.description(), p.departmentId(), p.level(), p.sequence(), positionUserCount(p), p.maxCount(), p.createdAt(), p.updatedAt());
    }

    private PoolUserView userView(PoolUserEntity e) {
        return new PoolUserView(e.id(), e.tenantId(), e.username(), e.email(), e.phone(), e.name(), e.avatar(), e.status(), e.emailVerified(), e.phoneVerified(), e.department(), e.position(), e.customAttributes(), e.lastLoginAt(), e.createdAt(), e.updatedAt());
    }

    private long memberCount(UUID tenant, UUID group) {
        return sql.createQuery(MEMBER).where(MEMBER.id().tenantId().eq(tenant), MEMBER.id().groupId().eq(group)).select(MEMBER.id()).fetchUnlimitedCount();
    }

    private long adminCount(UUID tenant, UUID group) {
        return sql.createQuery(ADMIN).where(ADMIN.id().tenantId().eq(tenant), ADMIN.id().groupId().eq(group)).select(ADMIN.id()).fetchUnlimitedCount();
    }

    private long childCount(UUID tenant, UUID parent) {
        return sql.createQuery(GROUP).where(GROUP.tenantId().eq(tenant), GROUP.parentId().eq(parent)).select(GROUP.id()).fetchUnlimitedCount();
    }

    private long positionUserCount(PositionEntity p) {
        return sql.createQuery(USER).where(USER.tenantId().eq(p.tenantId()), USER.position().eq(p.name())).select(USER.id()).fetchUnlimitedCount();
    }

    private void validateParent(UUID tenant, UUID self, UUID parent) {
        if (parent == null) {
            return;
        }
        if (parent.equals(self)) {
            throw new DomainException(ErrorCodeConstants.GROUP_PARENT_SELF);
        }
        var current = groupEntity(tenant, parent);
        Set<UUID> seen = new HashSet<>();
        while (current.parentId() != null) {
            if (!seen.add(current.id()) || current.parentId().equals(self)) {
                throw new DomainException(ErrorCodeConstants.GROUP_CYCLE);
            }
            current = groupEntity(tenant, current.parentId());
        }
    }

    private void validateGroup(String name, String type) {
        if (name == null || name.isBlank() || !Set.of("team", "department", "project", "organization").contains(type == null ? "team" : type)) {
            throw new DomainException(ErrorCodeConstants.GROUP_INVALID);
        }
    }

    private void validatePosition(String name, String code) {
        if (name == null || name.isBlank() || code == null || code.isBlank()) {
            throw new DomainException(ErrorCodeConstants.POSITION_INVALID);
        }
    }

    private static void rejectEnterpriseManaged(UserGroupEntity group) {
        if (group.enterpriseIdentitySourceId() != null) {
            throw new DomainException(ErrorCodeConstants.ENTERPRISE_IDENTITY_MANAGED_DEPARTMENT);
        }
    }

    private static long countType(List<UserGroupEntity> groups, String type) {
        return groups.stream().filter(g -> type.equals(g.type())).count();
    }

    /** 从当前租户上下文取租户 ID 后分页查询用户组。 */
    @Transactional(readOnly = true)
    public PageData<GroupView> groups(int page, int size, String name, String type, UUID parentId) {
        return groups(TenantContextHolder.requireTenantId(), page, size, name, type, parentId);
    }

    /** 从当前租户上下文取租户 ID 后查询用户组详情。 */
    @Transactional(readOnly = true)
    public GroupView group(UUID id) {
        return group(TenantContextHolder.requireTenantId(), id);
    }

    /** 从当前租户上下文取租户 ID 后创建用户组。 */
    @Transactional
    public GroupView createGroup(GroupInput in) {
        return createGroup(TenantContextHolder.requireTenantId(), in);
    }

    /** 从当前租户上下文取租户 ID 后更新用户组。 */
    @Transactional
    public GroupView updateGroup(UUID id, GroupInput in) {
        return updateGroup(TenantContextHolder.requireTenantId(), id, in);
    }

    /** 从当前租户上下文取租户 ID 后删除用户组。 */
    @Transactional
    public void deleteGroup(UUID id) {
        deleteGroup(TenantContextHolder.requireTenantId(), id);
    }

    /** 从当前租户上下文取租户 ID 后构建用户组树。 */
    @Transactional(readOnly = true)
    public List<GroupTree> groupTree() {
        return groupTree(TenantContextHolder.requireTenantId());
    }

    /** 从当前租户上下文取租户 ID 后统计用户组。 */
    @Transactional(readOnly = true)
    public GroupStats groupStats() {
        return groupStats(TenantContextHolder.requireTenantId());
    }

    /** 从当前租户上下文取租户 ID 后查询用户组成员与管理员。 */
    @Transactional(readOnly = true)
    public GroupMembers groupMembers(UUID groupId) {
        return groupMembers(TenantContextHolder.requireTenantId(), groupId);
    }

    /** 从当前租户上下文取租户 ID 后向用户组添加成员。 */
    @Transactional
    public void addMembers(UUID groupId, Collection<UUID> users) {
        addMembers(TenantContextHolder.requireTenantId(), groupId, users);
    }

    /** 从当前租户上下文取租户 ID 后移除用户组成员。 */
    @Transactional
    public void removeMembers(UUID groupId, Collection<UUID> users) {
        removeMembers(TenantContextHolder.requireTenantId(), groupId, users);
    }

    /** 从当前租户上下文取租户 ID 后向用户组添加管理员。 */
    @Transactional
    public void addAdmins(UUID groupId, Collection<UUID> users) {
        addAdmins(TenantContextHolder.requireTenantId(), groupId, users);
    }

    /** 从当前租户上下文取租户 ID 后移除用户组管理员。 */
    @Transactional
    public void removeAdmins(UUID groupId, Collection<UUID> users) {
        removeAdmins(TenantContextHolder.requireTenantId(), groupId, users);
    }

    /** 从当前租户上下文取租户 ID 后分页查询岗位。 */
    @Transactional(readOnly = true)
    public PageData<PositionView> positions(int page, int size, String name, String code, UUID departmentId, Integer level) {
        return positions(TenantContextHolder.requireTenantId(), page, size, name, code, departmentId, level);
    }

    /** 从当前租户上下文取租户 ID 后查询岗位详情。 */
    @Transactional(readOnly = true)
    public PositionView position(UUID id) {
        return position(TenantContextHolder.requireTenantId(), id);
    }

    /** 从当前租户上下文取租户 ID 后创建岗位。 */
    @Transactional
    public PositionView createPosition(PositionInput in) {
        return createPosition(TenantContextHolder.requireTenantId(), in);
    }

    /** 从当前租户上下文取租户 ID 后更新岗位。 */
    @Transactional
    public PositionView updatePosition(UUID id, PositionInput in) {
        return updatePosition(TenantContextHolder.requireTenantId(), id, in);
    }

    /** 从当前租户上下文取租户 ID 后删除岗位。 */
    @Transactional
    public void deletePosition(UUID id) {
        deletePosition(TenantContextHolder.requireTenantId(), id);
    }

    /** 从当前租户上下文取租户 ID 后统计岗位。 */
    @Transactional(readOnly = true)
    public PositionStats positionStats() {
        return positionStats(TenantContextHolder.requireTenantId());
    }

    /** 从当前租户上下文取租户 ID 后查询用户加入的用户组。 */
    @Transactional(readOnly = true)
    public List<GroupView> groupsForUser(UUID userId) {
        return groupsForUser(TenantContextHolder.requireTenantId(), userId);
    }

    /** 从当前租户上下文取租户 ID 后替换用户加入的用户组。 */
    @Transactional
    public void replaceUserGroups(UUID userId, Collection<UUID> groupIds) {
        replaceUserGroups(TenantContextHolder.requireTenantId(), userId, groupIds);
    }

    

    

    

    

    

    

    

    

    

    /** 用户组树的可变构建节点（组装完成后转为不可变的 {@link GroupTree}）。 */
    private static final class MutableGroupTree {
        final UserGroupEntity entity;
        final long members, admins;
        final List<MutableGroupTree> children = new ArrayList<>();

        MutableGroupTree(UserGroupEntity e, long m, long a) {
            entity = e;
            members = m;
            admins = a;
        }

        GroupTree freeze() {
            return new GroupTree(entity.id(), entity.name(), entity.description(), entity.type(), entity.parentId(), children.stream().map(MutableGroupTree::freeze).toList(), members, admins);
        }
    }
}
