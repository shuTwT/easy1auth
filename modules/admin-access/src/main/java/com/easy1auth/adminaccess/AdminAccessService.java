package com.easy1auth.adminaccess;

import com.easy1auth.adminaccess.model.*;
import com.easy1auth.adminidentity.AdminIdentityService;
import com.easy1auth.adminidentity.model.*;
import com.easy1auth.foundation.error.DomainException;
import com.easy1auth.foundation.id.UuidV7;
import com.easy1auth.foundation.web.PageData;
import com.easy1auth.tenant.TenantContext;
import com.easy1auth.tenant.model.*;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class AdminAccessService {
    private static final AdminRoleEntityTable ROLE = AdminRoleEntityTable.$;
    private static final TenantMembershipEntityTable MEMBERSHIP = TenantMembershipEntityTable.$;
    private static final AdminAccountEntityTable ACCOUNT = AdminAccountEntityTable.$;
    private final JSqlClient sql;
    private final ManagementPermissionCatalog catalog;
    private final AdminIdentityService identities;

    public AdminAccessService(JSqlClient sql, ManagementPermissionCatalog catalog, AdminIdentityService identities) {
        this.sql = sql;
        this.catalog = catalog;
        this.identities = identities;
    }

    @Transactional(readOnly = true)
    public PageData<AdminRoleView> roles(int page, int pageSize, String name, Boolean system) {
        int p = Math.max(1, page), size = Math.min(100, Math.max(1, pageSize));
        var query = sql.createQuery(ROLE).whereIf(name != null, () -> ROLE.name().ilike(name, LikeMode.ANYWHERE)).whereIf(system != null, () -> ROLE.systemRole().eq(system)).orderBy(ROLE.createdAt().desc()).select(ROLE.fetch(AdminRoleEntityFetcher.$.allScalarFields().permissions().memberships()));
        long total = query.fetchUnlimitedCount();
        return PageData.of(query.limit(size, (long) (p - 1) * size).execute().stream().map(this::view).toList(), p, size, total);
    }

    @Transactional(readOnly = true)
    public AdminRoleView role(UUID id) {
        return view(findEntityFetched(id));
    }

    @Transactional
    public AdminRoleView create(TenantContext c, String name, String description, List<String> permissions) {
        var codes = validate(name, permissions);
        requireGrantable(c, codes);
        UUID id = UuidV7.randomUuid();
        Instant now = Instant.now();
        sql.saveCommand(AdminRoleEntityDraft.$.produce(d -> d.setId(id).setTenantId(c.tenantId()).setName(name.strip()).setDescription(description).setSystemRole(false).setCreatedAt(now).setUpdatedAt(now))).setMode(SaveMode.INSERT_ONLY).execute();
        replacePermissions(id, codes);
        return find(c.tenantId(), id);
    }

    @Transactional
    public AdminRoleView update(TenantContext c, UUID id, String name, String description, List<String> permissions) {
        var old = findEntityFetched(c.tenantId(), id);
        if (old.systemRole()) throw immutable();
        var n = name == null ? old.name() : name;
        validateName(n);
        var codes = permissions == null ? catalog.validate(old.permissions().stream().map(ManagementPermissionEntity::code).toList(), ManagementPermissionScope.TENANT) : catalog.validate(permissions, ManagementPermissionScope.TENANT);
        requireGrantable(c, codes);
        var update = sql.createUpdate(ROLE).set(ROLE.name(), n.strip()).set(ROLE.updatedAt(), Instant.now()).where(ROLE.id().eq(id), ROLE.tenantId().eq(c.tenantId()));
        if (description != null) update.set(ROLE.description(), description);
        update.execute();
        replacePermissions(id, codes);
        return find(c.tenantId(), id);
    }

    @Transactional
    public void delete(UUID id) {
        if (findEntity(id).systemRole()) throw immutable();
        sql.deleteById(AdminRoleEntity.class, id);
    }

    @Transactional(readOnly = true)
    public PageData<AdminMemberView> members(int page, int pageSize, String username, String email, String status, UUID roleId) {
        int p = Math.max(1, page), size = Math.min(100, Math.max(1, pageSize));
        var memberships = activeMemberships();
        var byAccount = memberships.stream().collect(java.util.stream.Collectors.groupingBy(TenantMembershipEntity::accountId));
        var roleMemberships = roleId == null ? null : membersForRole(roleId);
        var allowed = roleMemberships == null ? null : memberships.stream().filter(m -> roleMemberships.contains(m.id())).map(TenantMembershipEntity::accountId).collect(java.util.stream.Collectors.toSet());
        var query = sql.createQuery(ACCOUNT).whereIf(allowed != null, () -> ACCOUNT.id().in(allowed)).whereIf(username != null, () -> ACCOUNT.username().ilike(username, LikeMode.ANYWHERE)).whereIf(email != null, () -> ACCOUNT.email().ilike(email, LikeMode.ANYWHERE)).whereIf(status != null, () -> ACCOUNT.status().eq(status)).orderBy(ACCOUNT.createdAt().desc(), ACCOUNT.id().asc()).select(ACCOUNT);
        long total = query.fetchUnlimitedCount();
        var rows = query.limit(size, (long) (p - 1) * size).execute().stream().map(a -> memberView(a, byAccount.getOrDefault(a.id(), List.of()))).toList();
        return PageData.of(rows, p, size, total);
    }

    @Transactional(readOnly = true)
    public AdminMemberView member(UUID account) {
        return memberView(findAccount(account), activeMemberships(account));
    }

    @Transactional
    public AdminRoleAssignmentView assignRoles(UUID tenant, UUID account, List<UUID> roleIds) {
        findAccount(account);
        var m = findMembership(tenant, account);
        var current = rolesForMembership(m.id());
        var associations = sql.getAssociations(AdminRoleEntityProps.MEMBERSHIPS);
        current.forEach(r -> associations.delete(r.id(), m.id()));
        for (UUID roleId : roleIds == null ? List.<UUID>of() : roleIds) {
            findEntity(tenant, roleId);
            associations.insert(roleId, m.id());
        }
        return dormantAssignment(account, tenant, m.id());
    }

    @Transactional
    public void updateMemberStatus(UUID actor, UUID account, String status) {
        if ("disabled".equals(status)) {
            identities.lockActive(account);
            var activeMemberships = sql.createQuery(MEMBERSHIP).where(MEMBERSHIP.accountId().eq(account), MEMBERSHIP.status().eq("active")).select(MEMBERSHIP.id()).forUpdate().execute();
            if (!activeMemberships.isEmpty())
                throw new DomainException(ErrorCodeConstants.ADMINISTRATOR_TRANSFER_REQUIRED);
        }
        identities.updateStatus(actor, account, status);
    }

    @Transactional
    public void ensureDefaultRoles(UUID tenant) {
        var codes = catalog.activeCodes(ManagementPermissionScope.TENANT);
        ensureSystemRole(tenant, "超级管理员", "拥有所有权限的系统管理员", codes);
        ensureSystemRole(tenant, "只读管理员", "仅拥有查看权限的系统管理员", codes.stream().filter(code -> code.value().endsWith(":read")).toList());
    }

    @Transactional(readOnly = true)
    public Map<String, Long> roleStats() {
        var roles = sql.createQuery(ROLE).select(ROLE.fetch(AdminRoleEntityFetcher.$.allScalarFields().memberships())).execute();
        return Map.of("totalRoles", (long) roles.size(), "systemRoles", roles.stream().filter(AdminRoleEntity::systemRole).count(), "customRoles", roles.stream().filter(r -> !r.systemRole()).count(), "totalAdmins", roles.stream().flatMap(r -> r.memberships().stream()).map(TenantMembershipEntity::id).distinct().count());
    }

    @Transactional(readOnly = true)
    public List<ManagementPermissionView> catalog() {
        return catalog.activeViews(ManagementPermissionScope.TENANT);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> memberStats() {
        var accounts = sql.createQuery(ACCOUNT).select(ACCOUNT).execute();
        var memberships = activeMemberships();
        return Map.of("totalAdmins", (long) accounts.size(), "activeAdmins", accounts.stream().filter(a -> "active".equals(a.status())).count(), "disabledAdmins", accounts.stream().filter(a -> "disabled".equals(a.status())).count(), "mfaEnabledAdmins", accounts.stream().filter(AdminAccountEntity::mfaEnabled).count(), "ownerCount", memberships.stream().filter(m -> "owner".equals(m.membershipRole())).map(TenantMembershipEntity::accountId).distinct().count());
    }

    private AdminRoleView find(UUID tenant, UUID id) {
        return view(findEntityFetched(tenant, id));
    }

    private AdminRoleEntity findEntity(UUID id) {
        return sql.createQuery(ROLE).where(ROLE.id().eq(id)).select(ROLE).fetchOptional().orElseThrow(() -> new DomainException(ErrorCodeConstants.ADMIN_ROLE_NOT_FOUND));
    }

    private AdminRoleEntity findEntityFetched(UUID id) {
        return sql.createQuery(ROLE).where(ROLE.id().eq(id)).select(ROLE.fetch(AdminRoleEntityFetcher.$.allScalarFields().permissions().memberships())).fetchOptional().orElseThrow(() -> new DomainException(ErrorCodeConstants.ADMIN_ROLE_NOT_FOUND));
    }

    private AdminRoleEntity findEntity(UUID tenant, UUID id) {
        return sql.createQuery(ROLE).where(ROLE.id().eq(id), ROLE.tenantId().eq(tenant)).select(ROLE).fetchOptional().orElseThrow(() -> new DomainException(ErrorCodeConstants.ADMIN_ROLE_NOT_FOUND));
    }

    private AdminRoleEntity findEntityFetched(UUID tenant, UUID id) {
        return sql.createQuery(ROLE).where(ROLE.id().eq(id), ROLE.tenantId().eq(tenant)).select(ROLE.fetch(AdminRoleEntityFetcher.$.allScalarFields().permissions().memberships())).fetchOptional().orElseThrow(() -> new DomainException(ErrorCodeConstants.ADMIN_ROLE_NOT_FOUND));
    }

    private Set<UUID> membersForRole(UUID role) {
        return sql.createQuery(ROLE).where(ROLE.id().eq(role)).select(ROLE.fetch(AdminRoleEntityFetcher.$.allScalarFields().memberships())).fetchOptional().orElseThrow(() -> new DomainException(ErrorCodeConstants.ADMIN_ROLE_NOT_FOUND)).memberships().stream().map(TenantMembershipEntity::id).collect(java.util.stream.Collectors.toSet());
    }

    private List<AdminRoleEntity> rolesForMembership(UUID membership) {
        return sql.createQuery(ROLE).where(ROLE.memberships(m -> m.id().eq(membership))).select(ROLE.fetch(AdminRoleEntityFetcher.$.allScalarFields().permissions())).execute();
    }

    private TenantMembershipEntity findMembership(UUID tenant, UUID account) {
        return sql.createQuery(MEMBERSHIP).where(MEMBERSHIP.tenantId().eq(tenant), MEMBERSHIP.accountId().eq(account), MEMBERSHIP.status().eq("active")).select(MEMBERSHIP).fetchOptional().orElseThrow(this::memberMissing);
    }

    private List<TenantMembershipEntity> activeMemberships() {
        return sql.createQuery(MEMBERSHIP).where(MEMBERSHIP.status().eq("active")).orderBy(MEMBERSHIP.createdAt().asc(), MEMBERSHIP.tenantId().asc(), MEMBERSHIP.id().asc()).select(MEMBERSHIP).execute();
    }

    private List<TenantMembershipEntity> activeMemberships(UUID account) {
        return sql.createQuery(MEMBERSHIP).where(MEMBERSHIP.accountId().eq(account), MEMBERSHIP.status().eq("active")).orderBy(MEMBERSHIP.createdAt().asc(), MEMBERSHIP.tenantId().asc(), MEMBERSHIP.id().asc()).select(MEMBERSHIP).execute();
    }

    private AdminAccountEntity findAccount(UUID account) {
        var entity = sql.findById(AdminAccountEntity.class, account);
        if (entity == null) throw new DomainException(ErrorCodeConstants.ADMIN_ACCOUNT_NOT_FOUND);
        return entity;
    }

    private AdminMemberView memberView(AdminAccountEntity account, List<TenantMembershipEntity> memberships) {
        var tenantViews = memberships.stream().map(this::membershipView).toList();
        var selected = memberships.stream().filter(m -> m.tenantId().equals(account.lastTenantId())).findFirst().or(() -> memberships.stream().findFirst());
        var tenantId = selected.map(TenantMembershipEntity::tenantId).orElse(null);
        var tenantRole = selected.map(TenantMembershipEntity::membershipRole).orElse(null);
        var roles = selected.map(m -> rolesForMembership(m.id()).stream().map(this::viewWithoutMemberships).toList()).orElse(List.of());
        return new AdminMemberView(account.id(), tenantId, tenantRole, account.lastTenantId(), account.username(), account.email(), account.phone(), account.status(), account.mfaEnabled(), account.mfaType(), account.lastLoginAt(), account.createdAt(), account.updatedAt(), roles, tenantViews, false, "dormant");
    }

    private AdminMembershipView membershipView(TenantMembershipEntity membership) {
        return new AdminMembershipView(membership.tenantId(), membership.membershipRole(), rolesForMembership(membership.id()).stream().map(this::viewWithoutMemberships).toList(), false, "dormant");
    }

    private AdminRoleAssignmentView dormantAssignment(UUID account, UUID tenant, UUID membership) {
        return new AdminRoleAssignmentView(account, tenant, rolesForMembership(membership).stream().map(this::viewWithoutMemberships).toList(), false, "dormant");
    }

    private AdminRoleView view(AdminRoleEntity r) {
        return new AdminRoleView(r.id(), r.tenantId(), r.name(), r.description(), r.permissions().stream().map(ManagementPermissionEntity::code).toList(), r.systemRole(), r.memberships().size(), r.createdAt(), r.updatedAt());
    }

    private AdminRoleView viewWithoutMemberships(AdminRoleEntity r) {
        return new AdminRoleView(r.id(), r.tenantId(), r.name(), r.description(), r.permissions().stream().map(ManagementPermissionEntity::code).toList(), r.systemRole(), 0, r.createdAt(), r.updatedAt());
    }

    private void ensureSystemRole(UUID tenant, String name, String description, List<ManagementPermissionCode> permissions) {
        boolean exists = sql.createQuery(ROLE).where(ROLE.tenantId().eq(tenant), ROLE.name().eq(name)).select(ROLE.id()).exists();
        if (!exists) {
            UUID id = UuidV7.randomUuid();
            Instant now = Instant.now();
            sql.saveCommand(AdminRoleEntityDraft.$.produce(d -> d.setId(id).setTenantId(tenant).setName(name).setDescription(description).setSystemRole(true).setCreatedAt(now).setUpdatedAt(now))).setMode(SaveMode.INSERT_ONLY).execute();
            replacePermissions(id, permissions);
        }
    }

    private List<ManagementPermissionCode> validate(String name, List<String> permissions) {
        validateName(name);
        return catalog.validate(permissions, ManagementPermissionScope.TENANT);
    }

    private void validateName(String name) {
        if (name == null || name.isBlank() || name.length() > 100)
            throw new DomainException(ErrorCodeConstants.ROLE_NAME_INVALID);
    }

    private void requireGrantable(TenantContext c, List<ManagementPermissionCode> permissions) {
        if (permissions.stream().map(ManagementPermissionCode::value).anyMatch(code -> !c.permissions().contains(code)))
            throw new DomainException(ErrorCodeConstants.PERMISSION_ESCALATION);
    }

    private void replacePermissions(UUID role, List<ManagementPermissionCode> permissions) {
        var associations = sql.getAssociations(AdminRoleEntityProps.PERMISSIONS);
        findEntityFetchedWithoutMemberships(role).permissions().forEach(permission -> associations.delete(role, permission.code()));
        permissions.forEach(permission -> associations.insert(role, permission.value()));
    }

    private AdminRoleEntity findEntityFetchedWithoutMemberships(UUID role) {
        return sql.createQuery(ROLE).where(ROLE.id().eq(role)).select(ROLE.fetch(AdminRoleEntityFetcher.$.permissions())).fetchOptional().orElseThrow(() -> new DomainException(ErrorCodeConstants.ADMIN_ROLE_NOT_FOUND));
    }

    private DomainException memberMissing() {
        return new DomainException(ErrorCodeConstants.ADMIN_MEMBER_NOT_FOUND);
    }

    private DomainException immutable() {
        return new DomainException(ErrorCodeConstants.SYSTEM_ROLE_IMMUTABLE);
    }

}
