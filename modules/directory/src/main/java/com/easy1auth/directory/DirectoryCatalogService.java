package com.easy1auth.directory;

import com.easy1auth.directory.model.*;
import com.easy1auth.foundation.error.DomainException;
import com.easy1auth.foundation.id.UuidV7;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DirectoryCatalogService {
    private static final UserGroupEntityTable GROUP = UserGroupEntityTable.$;
    private static final PositionEntityTable POSITION = PositionEntityTable.$;
    private static final PoolUserEntityTable USER = PoolUserEntityTable.$;
    private static final UserGroupAssignmentEntityTable MEMBER = UserGroupAssignmentEntityTable.$;
    private static final GroupAdminAssignmentEntityTable ADMIN = GroupAdminAssignmentEntityTable.$;
    private final JSqlClient sql;

    public DirectoryCatalogService(JSqlClient sql) { this.sql = sql; }

    @Transactional(readOnly = true)
    public Page<GroupView> groups(UUID tenant, int page, int size, String name, String type, UUID parentId) {
        int p = Math.max(1, page), s = Math.min(100, Math.max(1, size));
        var query = sql.createQuery(GROUP).where(GROUP.tenantId().eq(tenant))
                .whereIf(name != null, () -> GROUP.name().ilike(name, LikeMode.ANYWHERE))
                .whereIf(type != null, () -> GROUP.type().eq(type))
                .whereIf(parentId != null, () -> GROUP.parentId().eq(parentId))
                .orderBy(GROUP.createdAt().desc()).select(GROUP);
        long total = query.fetchUnlimitedCount();
        return new Page<>(query.limit(s, (long) (p - 1) * s).execute().stream().map(this::groupView).toList(), total, p, s);
    }

    @Transactional(readOnly = true) public GroupView group(UUID tenant, UUID id) { return groupView(groupEntity(tenant, id)); }

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

    @Transactional
    public GroupView updateGroup(UUID tenant, UUID id, GroupInput in) {
        var old = groupEntity(tenant, id);
        String name = in.name() == null ? old.name() : in.name();
        String type = in.type() == null ? old.type() : in.type();
        validateGroup(name, type); validateParent(tenant, id, in.parentId());
        var update = sql.createUpdate(GROUP).set(GROUP.name(), name).set(GROUP.type(), type).set(GROUP.updatedAt(), Instant.now())
                .where(GROUP.id().eq(id), GROUP.tenantId().eq(tenant));
        if (in.description() != null) update.set(GROUP.description(), in.description());
        if (in.parentId() != null) update.set(GROUP.parentId(), in.parentId());
        update.execute(); return group(tenant, id);
    }

    @Transactional
    public void deleteGroup(UUID tenant, UUID id) {
        groupEntity(tenant, id);
        if (sql.createQuery(GROUP).where(GROUP.tenantId().eq(tenant), GROUP.parentId().eq(id)).select(GROUP.id()).exists())
            throw new DomainException("GROUP_HAS_CHILDREN", "用户组下仍有子组，不能删除", 409);
        sql.createDelete(GROUP).where(GROUP.id().eq(id), GROUP.tenantId().eq(tenant)).execute();
    }

    @Transactional(readOnly = true)
    public List<GroupTree> groupTree(UUID tenant) {
        var groups = sql.createQuery(GROUP).where(GROUP.tenantId().eq(tenant)).orderBy(GROUP.createdAt()).select(GROUP).execute();
        Map<UUID, MutableGroupTree> nodes = new LinkedHashMap<>();
        groups.forEach(g -> nodes.put(g.id(), new MutableGroupTree(g, memberCount(g.tenantId(),g.id()), adminCount(g.tenantId(),g.id()))));
        List<MutableGroupTree> roots = new ArrayList<>();
        nodes.values().forEach(n -> { var parent = n.entity.parentId() == null ? null : nodes.get(n.entity.parentId()); if (parent == null) roots.add(n); else parent.children.add(n); });
        return roots.stream().map(MutableGroupTree::freeze).toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Long> groupStats(UUID tenant) {
        var rows = sql.createQuery(GROUP).where(GROUP.tenantId().eq(tenant)).select(GROUP).execute();
        return Map.of("totalGroups", (long) rows.size(), "teamGroups", countType(rows, "team"),
                "departmentGroups", countType(rows, "department"), "projectGroups", countType(rows, "project"),
                "organizationGroups", countType(rows, "organization"), "rootGroups", rows.stream().filter(g -> g.parentId() == null).count());
    }

    @Transactional(readOnly = true)
    public GroupMembers groupMembers(UUID tenant, UUID groupId) {
        groupEntity(tenant, groupId);
        var memberIds = sql.createQuery(MEMBER).where(MEMBER.id().tenantId().eq(tenant),MEMBER.id().groupId().eq(groupId)).select(MEMBER.id().userId()).execute();
        var adminIds = sql.createQuery(ADMIN).where(ADMIN.id().tenantId().eq(tenant),ADMIN.id().groupId().eq(groupId)).select(ADMIN.id().userId()).execute();
        return new GroupMembers(userViews(tenant, memberIds), userViews(tenant, adminIds), memberIds.size());
    }

    @Transactional public void addMembers(UUID tenant, UUID groupId, Collection<UUID> users) { mutateGroupUsers(tenant, groupId, users, false, true); }
    @Transactional public void removeMembers(UUID tenant, UUID groupId, Collection<UUID> users) { mutateGroupUsers(tenant, groupId, users, false, false); }
    @Transactional public void addAdmins(UUID tenant, UUID groupId, Collection<UUID> users) { mutateGroupUsers(tenant, groupId, users, true, true); }
    @Transactional public void removeAdmins(UUID tenant, UUID groupId, Collection<UUID> users) { mutateGroupUsers(tenant, groupId, users, true, false); }

    @Transactional(readOnly = true)
    public Page<PositionView> positions(UUID tenant, int page, int size, String name, String code, UUID departmentId, Integer level) {
        int p = Math.max(1, page), s = Math.min(100, Math.max(1, size));
        var query = sql.createQuery(POSITION).where(POSITION.tenantId().eq(tenant))
                .whereIf(name != null, () -> POSITION.name().ilike(name, LikeMode.ANYWHERE))
                .whereIf(code != null, () -> POSITION.code().ilike(code, LikeMode.ANYWHERE))
                .whereIf(departmentId != null, () -> POSITION.departmentId().eq(departmentId))
                .whereIf(level != null, () -> POSITION.level().eq(level)).orderBy(POSITION.createdAt().desc()).select(POSITION);
        long total = query.fetchUnlimitedCount();
        return new Page<>(query.limit(s, (long) (p - 1) * s).execute().stream().map(this::positionView).toList(), total, p, s);
    }

    @Transactional(readOnly = true) public PositionView position(UUID tenant, UUID id) { return positionView(positionEntity(tenant, id)); }

    @Transactional
    public PositionView createPosition(UUID tenant, PositionInput in) {
        validatePosition(in.name(), in.code()); Instant now = Instant.now();
        var e = PositionEntityDraft.$.produce(d -> d.setId(UuidV7.randomUuid()).setTenantId(tenant).setName(in.name()).setCode(in.code())
                .setDescription(in.description()).setDepartmentId(in.departmentId()).setLevel(in.level() == null ? 1 : in.level())
                .setSequence(in.sequence()).setMaxCount(in.maxCount()).setCreatedAt(now).setUpdatedAt(now));
        sql.saveCommand(e).setMode(SaveMode.INSERT_ONLY).execute(); return positionView(e);
    }

    @Transactional
    public PositionView updatePosition(UUID tenant, UUID id, PositionInput in) {
        var old = positionEntity(tenant, id); String name = in.name() == null ? old.name() : in.name(); String code = in.code() == null ? old.code() : in.code(); validatePosition(name, code);
        var u = sql.createUpdate(POSITION).set(POSITION.name(), name).set(POSITION.code(), code).set(POSITION.updatedAt(), Instant.now()).where(POSITION.id().eq(id), POSITION.tenantId().eq(tenant));
        if (in.description() != null) u.set(POSITION.description(), in.description()); if (in.departmentId() != null) u.set(POSITION.departmentId(), in.departmentId()); if (in.level() != null) u.set(POSITION.level(), in.level()); if (in.sequence() != null) u.set(POSITION.sequence(), in.sequence()); if (in.maxCount() != null) u.set(POSITION.maxCount(), in.maxCount()); u.execute(); return position(tenant, id);
    }

    @Transactional public void deletePosition(UUID tenant, UUID id) { positionEntity(tenant, id); sql.createDelete(POSITION).where(POSITION.id().eq(id), POSITION.tenantId().eq(tenant)).execute(); }

    @Transactional(readOnly = true)
    public Map<String, Object> positionStats(UUID tenant) {
        var rows = sql.createQuery(POSITION).where(POSITION.tenantId().eq(tenant)).select(POSITION).execute();
        long filled = rows.stream().filter(p -> positionUserCount(p) > 0).count();
        return Map.of("totalPositions", rows.size(), "filledPositions", filled, "vacantPositions", rows.size() - filled,
                "averageLevel", rows.stream().mapToInt(PositionEntity::level).average().orElse(0));
    }

    @Transactional(readOnly = true)
    public List<GroupView> groupsForUser(UUID tenant, UUID userId) { userEntity(tenant, userId); var ids = sql.createQuery(MEMBER).where(MEMBER.id().tenantId().eq(tenant),MEMBER.id().userId().eq(userId)).select(MEMBER.id().groupId()).execute(); return ids.isEmpty() ? List.of() : sql.createQuery(GROUP).where(GROUP.tenantId().eq(tenant), GROUP.id().in(ids)).select(GROUP).execute().stream().map(this::groupView).toList(); }

    @Transactional
    public void replaceUserGroups(UUID tenant, UUID userId, Collection<UUID> groupIds) {
        userEntity(tenant, userId); for (UUID groupId : groupIds) groupEntity(tenant, groupId);
        sql.createDelete(MEMBER).where(MEMBER.id().tenantId().eq(tenant),MEMBER.id().userId().eq(userId)).execute();
        groupIds.forEach(groupId -> insertMember(tenant,userId,groupId));
    }

    private void mutateGroupUsers(UUID tenant, UUID groupId, Collection<UUID> userIds, boolean admins, boolean add) {
        groupEntity(tenant, groupId); userIds.forEach(id -> userEntity(tenant, id));
        for (UUID userId : userIds) {
            if (admins) { if (add) insertAdmin(tenant,groupId,userId); else sql.createDelete(ADMIN).where(ADMIN.id().tenantId().eq(tenant),ADMIN.id().groupId().eq(groupId), ADMIN.id().userId().eq(userId)).execute(); }
            else { if (add) insertMember(tenant,userId,groupId); else sql.createDelete(MEMBER).where(MEMBER.id().tenantId().eq(tenant),MEMBER.id().groupId().eq(groupId), MEMBER.id().userId().eq(userId)).execute(); }
        }
    }

    private void insertMember(UUID tenant,UUID user,UUID group){var id=UserGroupAssignmentIdDraft.$.produce(d->d.setTenantId(tenant).setUserId(user).setGroupId(group));sql.saveCommand(UserGroupAssignmentEntityDraft.$.produce(d->d.setId(id))).setMode(SaveMode.INSERT_IF_ABSENT).execute();}
    private void insertAdmin(UUID tenant,UUID group,UUID user){var id=GroupAdminAssignmentIdDraft.$.produce(d->d.setTenantId(tenant).setGroupId(group).setUserId(user));sql.saveCommand(GroupAdminAssignmentEntityDraft.$.produce(d->d.setId(id))).setMode(SaveMode.INSERT_IF_ABSENT).execute();}
    private UserGroupEntity groupEntity(UUID tenant, UUID id) { return sql.createQuery(GROUP).where(GROUP.id().eq(id), GROUP.tenantId().eq(tenant)).select(GROUP).fetchOptional().orElseThrow(() -> missing("用户组")); }
    private PositionEntity positionEntity(UUID tenant, UUID id) { return sql.createQuery(POSITION).where(POSITION.id().eq(id), POSITION.tenantId().eq(tenant)).select(POSITION).fetchOptional().orElseThrow(() -> missing("岗位")); }
    private PoolUserEntity userEntity(UUID tenant, UUID id) { return sql.createQuery(USER).where(USER.id().eq(id), USER.tenantId().eq(tenant)).select(USER).fetchOptional().orElseThrow(() -> missing("用户")); }
    private List<PoolUserView> userViews(UUID tenant, Collection<UUID> ids) { if (ids.isEmpty()) return List.of(); return sql.createQuery(USER).where(USER.tenantId().eq(tenant), USER.id().in(ids)).select(USER).execute().stream().map(this::userView).toList(); }
    private GroupView groupView(UserGroupEntity g) { return new GroupView(g.id(), g.tenantId(), g.name(), g.description(), g.type(), g.parentId(), g.createdAt(), g.updatedAt(), new Counts(memberCount(g.tenantId(),g.id()), adminCount(g.tenantId(),g.id()), childCount(g.tenantId(), g.id()))); }
    private PositionView positionView(PositionEntity p) { return new PositionView(p.id(), p.tenantId(), p.name(), p.code(), p.description(), p.departmentId(), p.level(), p.sequence(), positionUserCount(p), p.maxCount(), p.createdAt(), p.updatedAt()); }
    private PoolUserView userView(PoolUserEntity e) { return new PoolUserView(e.id(), e.tenantId(), e.username(), e.email(), e.phone(), e.name(), e.avatar(), e.status(), e.emailVerified(), e.phoneVerified(), e.department(), e.position(), e.customAttributes(), e.lastLoginAt(), e.createdAt(), e.updatedAt()); }
    private long memberCount(UUID tenant,UUID group){return sql.createQuery(MEMBER).where(MEMBER.id().tenantId().eq(tenant),MEMBER.id().groupId().eq(group)).select(MEMBER.id()).fetchUnlimitedCount();}
    private long adminCount(UUID tenant,UUID group){return sql.createQuery(ADMIN).where(ADMIN.id().tenantId().eq(tenant),ADMIN.id().groupId().eq(group)).select(ADMIN.id()).fetchUnlimitedCount();}
    private long childCount(UUID tenant, UUID parent) { return sql.createQuery(GROUP).where(GROUP.tenantId().eq(tenant), GROUP.parentId().eq(parent)).select(GROUP.id()).fetchUnlimitedCount(); }
    private long positionUserCount(PositionEntity p) { return sql.createQuery(USER).where(USER.tenantId().eq(p.tenantId()), USER.position().eq(p.name())).select(USER.id()).fetchUnlimitedCount(); }
    private void validateParent(UUID tenant, UUID self, UUID parent) { if (parent == null) return; if (parent.equals(self)) throw new DomainException("GROUP_PARENT_SELF", "不能将自身设为父组", 400); var current = groupEntity(tenant, parent); Set<UUID> seen = new HashSet<>(); while (current.parentId() != null) { if (!seen.add(current.id()) || current.parentId().equals(self)) throw new DomainException("GROUP_CYCLE", "用户组层级不能形成循环", 400); current = groupEntity(tenant, current.parentId()); } }
    private void validateGroup(String name, String type) { if (name == null || name.isBlank() || !Set.of("team", "department", "project", "organization").contains(type == null ? "team" : type)) throw new DomainException("GROUP_INVALID", "用户组名称或类型无效", 400); }
    private void validatePosition(String name, String code) { if (name == null || name.isBlank() || code == null || code.isBlank()) throw new DomainException("POSITION_INVALID", "岗位名称和编码不能为空", 400); }
    private DomainException missing(String type) { return new DomainException("DIRECTORY_ITEM_NOT_FOUND", type + "不存在", 404); }
    private static long countType(List<UserGroupEntity> groups, String type) { return groups.stream().filter(g -> type.equals(g.type())).count(); }

    public record GroupInput(String name, String description, String type, UUID parentId) {}
    public record PositionInput(String name, String code, String description, UUID departmentId, Integer level, String sequence, Integer maxCount) {}
    public record Page<T>(List<T> data, long total, int page, int pageSize) {}
    public record GroupView(UUID id, UUID tenantId, String name, String description, String type, UUID parentId, Instant createdAt, Instant updatedAt, Counts _count) {}
    public record Counts(long members,long admins,long children){}
    public record PositionView(UUID id, UUID tenantId, String name, String code, String description, UUID departmentId, int level, String sequence, long userCount, Integer maxCount, Instant createdAt, Instant updatedAt) {}
    public record GroupMembers(List<PoolUserView> members, List<PoolUserView> admins, int total) {}
    public record GroupTree(UUID id, String name, String description, String type, UUID parentId, List<GroupTree> children, long memberCount, long adminCount) {}
    private static final class MutableGroupTree { final UserGroupEntity entity; final long members, admins; final List<MutableGroupTree> children = new ArrayList<>(); MutableGroupTree(UserGroupEntity e, long m, long a) { entity=e;members=m;admins=a; } GroupTree freeze(){return new GroupTree(entity.id(),entity.name(),entity.description(),entity.type(),entity.parentId(),children.stream().map(MutableGroupTree::freeze).toList(),members,admins);} }
}
