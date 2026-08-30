package com.easy1auth.adminaccess.service;

import com.easy1auth.adminaccess.*;
import com.easy1auth.adminaccess.constant.ErrorCodeConstants;
import com.easy1auth.adminaccess.constant.ManagementPermissionCode;
import com.easy1auth.adminaccess.constant.ManagementPermissionScope;
import com.easy1auth.adminaccess.dto.*;
import com.easy1auth.adminaccess.model.*;
import com.easy1auth.adminaccess.repository.AdminAccessRepository;
import com.easy1auth.adminidentity.model.*;
import com.easy1auth.adminidentity.service.AdminIdentityService;
import com.easy1auth.infrastructure.foundation.error.DomainException;
import com.easy1auth.infrastructure.foundation.id.UuidV7;
import com.easy1auth.infrastructure.foundation.web.PageData;
import com.easy1auth.tenant.model.*;
import com.easy1auth.tenant.util.TenantContext;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理端访问服务：管理角色（admin_role）与管理员成员（admin_user）的管理端 权限域业务。
 *
 * <p>提供管理角色的 CRUD 与权限绑定、角色分配、管理员成员查询与状态管理， 以及系统预置角色（超级管理员 / 只读管理员）的初始化。所有角色变更均先校验 权限的可授予性，防止越权授予。
 */
@Service
public class AdminAccessService {
  /** jimmer SQL 客户端 */
  private final AdminAccessRepository repository;

  /** 管理端权限目录 */
  private final ManagementPermissionCatalog catalog;

  /** 管理账号身份服务 */
  private final AdminIdentityService identities;

  public AdminAccessService(
      AdminAccessRepository repository,
      ManagementPermissionCatalog catalog,
      AdminIdentityService identities) {
    this.repository = repository;
    this.catalog = catalog;
    this.identities = identities;
  }

  /** 分页查询管理角色列表（支持按名称模糊、是否系统角色过滤）。 */
  @Transactional(readOnly = true)
  public PageData<AdminRoleView> roles(int page, int pageSize, String name, Boolean system) {
    int p = Math.max(1, page), size = Math.min(100, Math.max(1, pageSize));
    var result = repository.pageRoles(p, size, name, system);
    return PageData.of(result.items().stream().map(this::view).toList(), p, size, result.total());
  }

  /** 查询单个管理角色详情。 */
  @Transactional(readOnly = true)
  public AdminRoleView role(UUID id) {
    return view(findEntityFetched(id));
  }

  /** 在租户下创建自定义管理角色并绑定权限（系统角色标记为 false）。 */
  @Transactional
  public AdminRoleView create(
      TenantContext c, String name, String description, List<String> permissions) {
    var codes = validate(name, permissions);
    requireGrantable(c, codes);
    UUID id = UuidV7.randomUuid();
    Instant now = Instant.now();
    repository.saveRole(
        AdminRoleEntityDraft.$.produce(
            d ->
                d.setId(id)
                    .setTenantId(c.tenantId())
                    .setName(name.strip())
                    .setDescription(description)
                    .setSystemRole(false)
                    .setCreatedAt(now)
                    .setUpdatedAt(now)));
    replacePermissions(id, codes);
    return find(c.tenantId(), id);
  }

  /** 更新管理角色名称、描述与权限（系统角色不可修改）。 */
  @Transactional
  public AdminRoleView update(
      TenantContext c, UUID id, String name, String description, List<String> permissions) {
    var old = findEntityFetched(c.tenantId(), id);
    if (old.systemRole()) {
      throw immutable();
    }
    var n = name == null ? old.name() : name;
    validateName(n);
    var codes =
        permissions == null
            ? catalog.validate(
                old.permissions().stream().map(ManagementPermissionEntity::code).toList(),
                ManagementPermissionScope.TENANT)
            : catalog.validate(permissions, ManagementPermissionScope.TENANT);
    requireGrantable(c, codes);
    repository.updateRole(c.tenantId(), id, n.strip(), description);
    replacePermissions(id, codes);
    return find(c.tenantId(), id);
  }

  /** 删除自定义管理角色（系统角色不可删除）。 */
  @Transactional
  public void delete(UUID id) {
    if (findEntity(id).systemRole()) {
      throw immutable();
    }
    repository.deleteRole(id);
  }

  /** 分页查询管理员成员列表（支持按账号/邮箱/状态/角色过滤）。 */
  @Transactional(readOnly = true)
  public PageData<AdminMemberView> members(
      int page, int pageSize, String username, String email, String status, UUID roleId) {
    int p = Math.max(1, page), size = Math.min(100, Math.max(1, pageSize));
    var memberships = activeMemberships();
    var byAccount =
        memberships.stream()
            .collect(java.util.stream.Collectors.groupingBy(TenantMembershipEntity::accountId));
    var roleMemberships = roleId == null ? null : membersForRole(roleId);
    var allowed =
        roleMemberships == null
            ? null
            : memberships.stream()
                .filter(m -> roleMemberships.contains(m.id()))
                .map(TenantMembershipEntity::accountId)
                .collect(java.util.stream.Collectors.toSet());
    var result = repository.pageAccounts(p, size, allowed, username, email, status);
    var rows =
        result.items().stream()
            .map(a -> memberView(a, byAccount.getOrDefault(a.id(), List.of())))
            .toList();
    return PageData.of(rows, p, size, result.total());
  }

  /** 查询单个管理员成员详情（含名下各租户的成员关系与角色）。 */
  @Transactional(readOnly = true)
  public AdminMemberView member(UUID account) {
    return memberView(findAccount(account), activeMemberships(account));
  }

  /** 为账号在指定租户下重设角色分配（先清空已有分配再写入）。 */
  @Transactional
  public AdminRoleAssignmentView assignRoles(UUID tenant, UUID account, List<UUID> roleIds) {
    findAccount(account);
    var m = findMembership(tenant, account);
    var current = rolesForMembership(m.id());
    for (UUID roleId : roleIds == null ? List.<UUID>of() : roleIds) {
      findEntity(tenant, roleId);
    }
    repository.replaceMembershipRoles(m.id(), roleIds == null ? List.of() : roleIds);
    return dormantAssignment(account, tenant, m.id());
  }

  /** 更新管理员账号状态；停用时校验其名下仍无其他可用管理员，否则拒绝。 */
  @Transactional
  public void updateMemberStatus(UUID actor, UUID account, String status) {
    if ("disabled".equals(status)) {
      identities.lockActive(account);
      var activeMemberships = repository.findActiveMembershipIdsForUpdate(account);
      if (!activeMemberships.isEmpty()) {
        throw new DomainException(ErrorCodeConstants.ADMINISTRATOR_TRANSFER_REQUIRED);
      }
    }
    identities.updateStatus(actor, account, status);
  }

  /** 确保租户存在系统预置角色：超级管理员（全部权限）与只读管理员（仅查看权限）。 */
  @Transactional
  public void ensureDefaultRoles(UUID tenant) {
    var codes = catalog.activeCodes(ManagementPermissionScope.TENANT);
    ensureSystemRole(tenant, "超级管理员", "拥有所有权限的系统管理员", codes);
    ensureSystemRole(
        tenant,
        "只读管理员",
        "仅拥有查看权限的系统管理员",
        codes.stream().filter(code -> code.value().endsWith(":read")).toList());
  }

  /** 统计角色总量、系统角色数、自定义角色数与关联管理员数。 */
  @Transactional(readOnly = true)
  public RoleStats roleStats() {
    var roles = repository.findRolesWithMemberships();
    return new RoleStats(
        roles.size(),
        roles.stream().filter(AdminRoleEntity::systemRole).count(),
        roles.stream().filter(r -> !r.systemRole()).count(),
        roles.stream()
            .flatMap(r -> r.memberships().stream())
            .map(TenantMembershipEntity::id)
            .distinct()
            .count());
  }

  /** 返回租户作用域下全部启用权限的视图列表（角色授权界面使用）。 */
  @Transactional(readOnly = true)
  public List<ManagementPermissionView> catalog() {
    return catalog.activeViews(ManagementPermissionScope.TENANT);
  }

  /** 统计管理员成员总量、启用/停用数、MFA 启用数与 owner 角色账号数。 */
  @Transactional(readOnly = true)
  public MemberStats memberStats() {
    var accounts = repository.findAccounts();
    var memberships = activeMemberships();
    return new MemberStats(
        accounts.size(),
        accounts.stream().filter(a -> "active".equals(a.status())).count(),
        accounts.stream().filter(a -> "disabled".equals(a.status())).count(),
        accounts.stream().filter(AdminAccountEntity::mfaEnabled).count(),
        memberships.stream()
            .filter(m -> "owner".equals(m.membershipRole()))
            .map(TenantMembershipEntity::accountId)
            .distinct()
            .count());
  }

  /** 查询租户内角色并转换为视图。 */
  private AdminRoleView find(UUID tenant, UUID id) {
    return view(findEntityFetched(tenant, id));
  }

  /** 按 ID 查询角色实体，不存在时抛出领域异常。 */
  private AdminRoleEntity findEntity(UUID id) {
    return repository
        .findRole(id)
        .orElseThrow(() -> new DomainException(ErrorCodeConstants.ADMIN_ROLE_NOT_FOUND));
  }

  /** 按 ID 查询角色实体（含权限与成员关联），不存在时抛出领域异常。 */
  private AdminRoleEntity findEntityFetched(UUID id) {
    return repository
        .findRoleFetched(id)
        .orElseThrow(() -> new DomainException(ErrorCodeConstants.ADMIN_ROLE_NOT_FOUND));
  }

  /** 按租户与 ID 查询角色实体，不存在时抛出领域异常。 */
  private AdminRoleEntity findEntity(UUID tenant, UUID id) {
    return repository
        .findRole(tenant, id)
        .orElseThrow(() -> new DomainException(ErrorCodeConstants.ADMIN_ROLE_NOT_FOUND));
  }

  /** 按租户与 ID 查询角色实体（含权限与成员关联），不存在时抛出领域异常。 */
  private AdminRoleEntity findEntityFetched(UUID tenant, UUID id) {
    return repository
        .findRoleFetched(tenant, id)
        .orElseThrow(() -> new DomainException(ErrorCodeConstants.ADMIN_ROLE_NOT_FOUND));
  }

  /** 查询角色关联的成员关系 ID 集合（用于按角色过滤成员）。 */
  private Set<UUID> membersForRole(UUID role) {
    return repository
        .findRoleFetched(role)
        .orElseThrow(() -> new DomainException(ErrorCodeConstants.ADMIN_ROLE_NOT_FOUND))
        .memberships()
        .stream()
        .map(TenantMembershipEntity::id)
        .collect(java.util.stream.Collectors.toSet());
  }

  /** 查询某成员关系被分配的全部角色。 */
  private List<AdminRoleEntity> rolesForMembership(UUID membership) {
    return repository.findRolesForMembership(membership);
  }

  /** 查询账号在租户内的激活成员关系，不存在时抛出领域异常。 */
  private TenantMembershipEntity findMembership(UUID tenant, UUID account) {
    return repository.findActiveMembership(tenant, account).orElseThrow(this::memberMissing);
  }

  /** 查询全部激活成员关系（按创建时间、租户、ID 稳定排序）。 */
  private List<TenantMembershipEntity> activeMemberships() {
    return repository.findActiveMemberships();
  }

  /** 查询某账号名下全部激活成员关系。 */
  private List<TenantMembershipEntity> activeMemberships(UUID account) {
    return repository.findActiveMemberships(account);
  }

  /** 按 ID 查询管理账号，不存在时抛出领域异常。 */
  private AdminAccountEntity findAccount(UUID account) {
    return repository
        .findAccount(account)
        .orElseThrow(() -> new DomainException(ErrorCodeConstants.ADMIN_ACCOUNT_NOT_FOUND));
  }

  /** 组装管理员成员视图：选取当前选中租户（或首个租户）的角色信息。 */
  private AdminMemberView memberView(
      AdminAccountEntity account, List<TenantMembershipEntity> memberships) {
    var tenantViews = memberships.stream().map(this::membershipView).toList();
    var selected =
        memberships.stream()
            .filter(m -> m.tenantId().equals(account.lastTenantId()))
            .findFirst()
            .or(() -> memberships.stream().findFirst());
    var tenantId = selected.map(TenantMembershipEntity::tenantId).orElse(null);
    var tenantRole = selected.map(TenantMembershipEntity::membershipRole).orElse(null);
    var roles =
        selected
            .map(
                m -> rolesForMembership(m.id()).stream().map(this::viewWithoutMemberships).toList())
            .orElse(List.of());
    return new AdminMemberView(
        account.id(),
        tenantId,
        tenantRole,
        account.lastTenantId(),
        account.username(),
        account.email(),
        account.phone(),
        account.status(),
        account.mfaEnabled(),
        account.mfaType(),
        account.lastLoginAt(),
        account.createdAt(),
        account.updatedAt(),
        roles,
        tenantViews,
        false,
        "dormant");
  }

  /** 组装单个成员关系视图（含该租户下被分配的角色）。 */
  private AdminMembershipView membershipView(TenantMembershipEntity membership) {
    return new AdminMembershipView(
        membership.tenantId(),
        membership.membershipRole(),
        rolesForMembership(membership.id()).stream().map(this::viewWithoutMemberships).toList(),
        false,
        "dormant");
  }

  /** 组装角色分配视图（授权状态暂以 "dormant" 占位）。 */
  private AdminRoleAssignmentView dormantAssignment(UUID account, UUID tenant, UUID membership) {
    return new AdminRoleAssignmentView(
        account,
        tenant,
        rolesForMembership(membership).stream().map(this::viewWithoutMemberships).toList(),
        false,
        "dormant");
  }

  /** 将角色实体转换为视图（含成员数与权限码）。 */
  private AdminRoleView view(AdminRoleEntity r) {
    return new AdminRoleView(
        r.id(),
        r.tenantId(),
        r.name(),
        r.description(),
        r.permissions().stream().map(ManagementPermissionEntity::code).toList(),
        r.systemRole(),
        r.memberships().size(),
        r.createdAt(),
        r.updatedAt());
  }

  /** 将角色实体转换为视图（不填充成员数，用于嵌套场景）。 */
  private AdminRoleView viewWithoutMemberships(AdminRoleEntity r) {
    return new AdminRoleView(
        r.id(),
        r.tenantId(),
        r.name(),
        r.description(),
        r.permissions().stream().map(ManagementPermissionEntity::code).toList(),
        r.systemRole(),
        0,
        r.createdAt(),
        r.updatedAt());
  }

  /** 确保租户存在指定系统预置角色，不存在则创建并绑定权限。 */
  private void ensureSystemRole(
      UUID tenant, String name, String description, List<ManagementPermissionCode> permissions) {
    boolean exists = repository.roleExists(tenant, name);
    if (!exists) {
      UUID id = UuidV7.randomUuid();
      Instant now = Instant.now();
      repository.saveRole(
          AdminRoleEntityDraft.$.produce(
              d ->
                  d.setId(id)
                      .setTenantId(tenant)
                      .setName(name)
                      .setDescription(description)
                      .setSystemRole(true)
                      .setCreatedAt(now)
                      .setUpdatedAt(now)));
      replacePermissions(id, permissions);
    }
  }

  /** 校验角色名称并校验权限码（租户作用域）。 */
  private List<ManagementPermissionCode> validate(String name, List<String> permissions) {
    validateName(name);
    return catalog.validate(permissions, ManagementPermissionScope.TENANT);
  }

  /** 校验角色名称非空且不超过 100 字。 */
  private void validateName(String name) {
    if (name == null || name.isBlank() || name.length() > 100) {
      throw new DomainException(ErrorCodeConstants.ROLE_NAME_INVALID);
    }
  }

  /** 校验权限均为当前上下文可授予的权限，否则抛出越权异常。 */
  private void requireGrantable(TenantContext c, List<ManagementPermissionCode> permissions) {
    if (permissions.stream()
        .map(ManagementPermissionCode::value)
        .anyMatch(code -> !c.permissions().contains(code))) {
      throw new DomainException(ErrorCodeConstants.PERMISSION_ESCALATION);
    }
  }

  /** 全量替换角色的权限关联（先清空再写入）。 */
  private void replacePermissions(UUID role, List<ManagementPermissionCode> permissions) {
    repository.replacePermissions(
        role, permissions.stream().map(ManagementPermissionCode::value).toList());
  }

  /** 按 ID 查询角色实体（仅含权限关联），用于替换权限。 */
  private AdminRoleEntity findEntityFetchedWithoutMemberships(UUID role) {
    return repository
        .findRoleWithPermissions(role)
        .orElseThrow(() -> new DomainException(ErrorCodeConstants.ADMIN_ROLE_NOT_FOUND));
  }

  /** 构造管理员成员缺失的领域异常。 */
  private DomainException memberMissing() {
    return new DomainException(ErrorCodeConstants.ADMIN_MEMBER_NOT_FOUND);
  }

  /** 构造系统角色不可修改/删除的领域异常。 */
  private DomainException immutable() {
    return new DomainException(ErrorCodeConstants.SYSTEM_ROLE_IMMUTABLE);
  }
}
