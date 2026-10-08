package com.easy1auth.tenant.service;

import com.easy1auth.framework.common.error.DomainException;
import com.easy1auth.tenant.spi.ActiveAdminAccountLocker;
import com.easy1auth.tenant.*;
import com.easy1auth.tenant.constant.ErrorCodeConstants;
import com.easy1auth.tenant.dto.UserPoolControlView;
import com.easy1auth.tenant.dto.UserPoolAccess;
import com.easy1auth.tenant.dto.UserPoolPackageView;
import com.easy1auth.tenant.dto.UserPoolSummary;
import com.easy1auth.tenant.model.UserPoolEntity;
import com.easy1auth.tenant.infrastructure.repository.UserPoolRepository;
import com.easy1auth.tenant.dto.UserPoolState;
import com.easy1auth.tenant.dto.UserPoolAuthorizationRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 用户池服务：用户池生命周期管理（创建、更新、状态变更、删除、管理员转移），
 * 以及用户池上下文解析、可用套餐查询与配额锁定。
 *
 * <p>所有变更操作均处于事务边界内，并在执行前完成账号与套餐的并发锁定，
 * 以保证多用户池配额与管理员身份的一致性。</p>
 */
@Service
public class UserPoolService {
    /** 用户池数据访问仓储 */
    private final UserPoolRepository repository;
    /** 用户池访问授权解析器（解析权限与套餐） */
    private final UserPoolAuthorizationProvider authorization;
    /** 用户池套餐服务 */
    private final UserPoolPackageService packages;
    /** 管理账号锁定器（防止并发操作正在被变更的账号） */
    private final ActiveAdminAccountLocker administratorAccounts;

    UserPoolService(UserPoolRepository repository, UserPoolAuthorizationProvider authorization, UserPoolPackageService packages,
                  ActiveAdminAccountLocker administratorAccounts) {
        this.repository = repository;
        this.authorization = authorization;
        this.packages = packages;
        this.administratorAccounts = administratorAccounts;
    }

    /** 查询指定管理账号可访问的用户池摘要列表（含账号在各用户池中的角色）。 */
    @Transactional(readOnly = true)
    public List<UserPoolSummary> list(UUID accountId) {
        return repository.listForAccount(accountId).stream().map(this::summary).toList();
    }

    /** 查询全部用户池摘要（平台管理视角，标注每个用户池的默认角色）。 */
    @Transactional(readOnly = true)
    public List<UserPoolSummary> listAll() {
        return repository.listAllUserPools().stream().map(this::managementSummary).toList();
    }

    /** 查询全部普通用户池的控制视图（用于平台侧用户池管理列表）。 */
    @Transactional(readOnly = true)
    public List<UserPoolControlView> listManaged() {
        return repository.listOrdinaryUserPools().stream().map(this::controlView).toList();
    }

    /** 查询可分配给普通用户池的启用中套餐列表。 */
    @Transactional(readOnly = true)
    public List<UserPoolPackageView> listAssignablePackages() {
        return packages.list().stream()
                .filter(item -> "active".equals(item.status()))
                .toList();
    }

    /**
     * 解析用户池上下文：校验账号在该用户池的有效成员关系与角色，
     * 并解析出该用户池下的权限集合与套餐信息。
     *
     * @param traceId 链路追踪 ID（透传，用于日志串联）
     * @return 用户池上下文，供后续请求的权限判断使用
     */
    @Transactional(readOnly = true)
    public UserPoolAccess authorize(UUID accountId, UUID userPoolId) {
        var membership = repository.activeMembership(accountId, userPoolId).filter(it -> authorization.isActiveAccount(accountId)).orElseThrow(() -> new DomainException(ErrorCodeConstants.USER_POOL_ACCESS_DENIED));
        validateMembershipRole(membership.system(), membership.role());
        var effective = authorization.resolve(new UserPoolAuthorizationRequest(
                accountId, userPoolId, membership.id(), membership.role(), membership.system(), membership.packageId()));
        return new UserPoolAccess(
                userPoolId,
                membership.role(),
                effective.permissions(),
                effective.userPoolPackage());
    }

    /** 创建普通用户池并绑定指定套餐，同时将指定管理账号设为用户池管理员。 */
    @Transactional
    public UserPoolSummary createOrdinary(String name, long packageId, UUID administratorAccountId) {
        var userPoolPackage = packages.lockActiveAssignable(packageId);
        administratorAccounts.lockActive(administratorAccountId);
        return createOrdinary(name, userPoolPackage, administratorAccountId);
    }

    /** 创建普通用户池并绑定默认可用套餐，同时将指定管理账号设为用户池管理员。 */
    @Transactional
    public UserPoolSummary createOrdinaryWithDefaultPackage(String name, UUID administratorAccountId) {
        var userPoolPackage = packages.lockActiveDefaultAssignable();
        administratorAccounts.lockActive(administratorAccountId);
        return createOrdinary(name, userPoolPackage, administratorAccountId);
    }

    /** 创建唯一的系统用户池，并将首个系统管理员设为 super_admin。 */
    @Transactional
    public UserPoolSummary createSystem(UUID administratorAccountId) {
        administratorAccounts.lockActive(administratorAccountId);
        UserPoolEntity tenant = repository.createSystemUserPool("系统用户池");
        String role = administratorRole(true);
        repository.createActiveAdministratorMembership(tenant.id(), administratorAccountId, role);
        return new UserPoolSummary(
                tenant.id(),
                tenant.name(),
                tenant.status(),
                true,
                packages.systemPackage(),
                role);
    }

    /** 更新普通用户池名称或绑定的套餐（仅更新传入的非空字段）。 */
    @Transactional
    public UserPoolControlView updateOrdinary(UUID userPoolId, String name, Long packageId) {
        UserPoolEntity existing = requiredOrdinaryUserPool(userPoolId);
        String normalizedName = name == null ? existing.name() : normalizeName(name);
        long assignedPackageId = packageId == null
                ? Objects.requireNonNull(existing.packageInfo(), "ordinary tenant package missing").id()
                : packages.lockActiveAssignable(packageId).id();
        repository.updateOrdinaryUserPool(existing.id(), normalizedName, assignedPackageId);
        return controlView(requiredOrdinaryUserPool(existing.id()));
    }

    /** 更新普通用户池状态（active / suspended）；恢复为 active 时校验套餐仍可分配。 */
    @Transactional
    public UserPoolControlView updateOrdinaryStatus(UUID userPoolId, String status) {
        UserPoolEntity tenant = requiredOrdinaryUserPool(userPoolId);
        String normalized = status == null ? "" : status.strip();
        if (!"active".equals(normalized) && !"suspended".equals(normalized)) {
            throw new DomainException(ErrorCodeConstants.USER_POOL_STATUS_INVALID);
        }
        if ("active".equals(normalized)) {
            packages.lockActiveAssignable(Objects.requireNonNull(tenant.packageInfo(), "ordinary tenant package missing").id());
        }
        repository.updateUserPoolStatus(tenant.id(), normalized);
        return controlView(requiredOrdinaryUserPool(tenant.id()));
    }

    /** 删除普通用户池（软删除）：置为 deleted 状态并停用其全部成员关系。 */
    @Transactional
    public void deleteOrdinary(UUID userPoolId) {
        UserPoolEntity tenant = requiredOrdinaryUserPool(userPoolId);
        repository.updateUserPoolStatus(tenant.id(), "deleted");
        repository.suspendAllMemberships(tenant.id());
    }

    /** 转移用户池管理员：将当前管理员降为普通成员，并将目标账号提升为用户池管理员。 */
    @Transactional
    public UserPoolControlView transferAdministrator(UUID userPoolId, UUID targetAccountId) {
        if (targetAccountId == null) {
            throw new DomainException(ErrorCodeConstants.ADMINISTRATOR_ACCOUNT_REQUIRED);
        }
        UserPoolEntity tenant = requiredOrdinaryUserPool(userPoolId);
        if (!"active".equals(tenant.status())) {
            throw new DomainException(ErrorCodeConstants.USER_POOL_NOT_ACTIVE);
        }
        String administratorRole = administratorRole(false);
        administratorAccounts.lockActive(targetAccountId);
        var current = repository.lockActiveAdministratorMembership(tenant.id())
                .orElseThrow(() -> new DomainException(ErrorCodeConstants.USER_POOL_ADMINISTRATOR_MISSING));
        if (current.accountId().equals(targetAccountId)) {
            throw new DomainException(ErrorCodeConstants.USER_POOL_ADMINISTRATOR_TARGET_CURRENT);
        }
        var targetMembership = repository.lockMembership(tenant.id(), targetAccountId);
        if (targetMembership.isPresent()) {
            if ("active".equals(targetMembership.get().status())) {
                repository.promoteActiveMembership(targetMembership.get().id(), administratorRole);
            } else {
                repository.activateAdministratorMembership(targetMembership.get().id(), administratorRole);
            }
        } else {
            repository.createActiveAdministratorMembership(tenant.id(), targetAccountId, administratorRole);
        }
        repository.suspendMembership(current.id());
        return controlView(requiredOrdinaryUserPool(tenant.id()));
    }

    /** 锁定用户池并返回其套餐允许的最大用户数（创建用户前的配额校验）。 */
    @Transactional
    public int lockForUserQuota(UUID userPoolId) {
        return repository.lockAndGetMaxUsers(userPoolId);
    }

    /** 锁定用户池并返回其套餐允许的最大应用数（创建应用前的配额校验）。 */
    @Transactional
    public int lockForAppQuota(UUID userPoolId) {
        return repository.lockAndGetMaxApps(userPoolId);
    }

    /** 锁定激活中的用户池，用于生成安全材料（如签名密钥）前的并发保护。 */
    @Transactional
    public void lockForSecurityMaterial(UUID userPoolId) {
        repository.lockActiveUserPool(userPoolId).orElseThrow(() -> new DomainException(ErrorCodeConstants.USER_POOL_NOT_FOUND_ACTIVE));
    }

    /** 将仓储返回的用户池状态转换为账号视角的用户池摘要。 */
    private UserPoolSummary summary(UserPoolState state) {
        var tenant = state.tenant();
        validateMembershipRole(tenant.isSystem(), state.role());
        var userPoolPackage = tenant.isSystem()
                ? packages.systemPackage()
                : packages.view(Objects.requireNonNull(tenant.packageInfo(), "ordinary tenant package missing"));
        return new UserPoolSummary(tenant.id(), tenant.name(), tenant.status(), tenant.isSystem(), userPoolPackage, state.role());
    }

    /** 将用户池实体转换为平台管理视角的用户池摘要（标注默认角色）。 */
    private UserPoolSummary managementSummary(UserPoolEntity tenant) {
        var userPoolPackage = tenant.isSystem()
                ? packages.systemPackage()
                : packages.view(Objects.requireNonNull(tenant.packageInfo(), "ordinary tenant package missing"));
        return new UserPoolSummary(tenant.id(), tenant.name(), tenant.status(), tenant.isSystem(), userPoolPackage,
                tenant.isSystem() ? "super_admin" : "tenant_admin");
    }

    /** 将用户池实体转换为控制视图（含当前管理员账号 ID）。 */
    private UserPoolControlView controlView(UserPoolEntity tenant) {
        var userPoolPackage = packages.view(Objects.requireNonNull(tenant.packageInfo(), "ordinary tenant package missing"));
        UUID administratorAccountId = repository.activeAdministratorMembership(tenant.id())
                .map(membership -> membership.accountId())
                .orElse(null);
        return new UserPoolControlView(tenant.id(), tenant.name(), tenant.status(), userPoolPackage, administratorAccountId);
    }

    /** 按 ID 锁定并返回普通用户池，不存在或已被删除时抛出领域异常。 */
    private UserPoolEntity requiredOrdinaryUserPool(UUID userPoolId) {
        if (userPoolId == null) {
            throw new DomainException(ErrorCodeConstants.USER_POOL_ID_REQUIRED);
        }
        return repository.lockOrdinaryUserPool(userPoolId)
                .orElseThrow(() -> new DomainException(ErrorCodeConstants.USER_POOL_NOT_FOUND_ORDINARY));
    }

    /** 创建普通用户池的内部实现（调用方须先完成套餐与账号锁定）。 */
    private UserPoolSummary createOrdinary(String name, UserPoolPackageView userPoolPackage, UUID administratorAccountId) {
        String normalizedName = normalizeName(name);
        UserPoolEntity tenant = repository.createOrdinaryUserPool(normalizedName, userPoolPackage.id());
        repository.createActiveAdministratorMembership(tenant.id(), administratorAccountId, administratorRole(false));
        return new UserPoolSummary(tenant.id(), tenant.name(), tenant.status(), false, userPoolPackage, administratorRole(false));
    }

    /** 返回指定类型用户池的管理员角色名（系统用户池 super_admin，普通用户池 tenant_admin）。 */
    private static String administratorRole(boolean systemUserPool) {
        return systemUserPool ? "super_admin" : "tenant_admin";
    }

    /** 校验成员角色与用户池类型是否匹配，不匹配时抛出领域异常。 */
    private static void validateMembershipRole(boolean systemUserPool, String membershipRole) {
        if ("common".equals(membershipRole)) {
            return;
        }
        if ((systemUserPool && "super_admin".equals(membershipRole))
                || (!systemUserPool && "tenant_admin".equals(membershipRole))) {
            return;
        }
        throw new DomainException(ErrorCodeConstants.USER_POOL_MEMBERSHIP_ROLE_INVALID);
    }

    /** 规范化用户池名称：去除首尾空白并校验长度（1-200）。 */
    private static String normalizeName(String name) {
        String normalized = name == null ? "" : name.strip();
        if (normalized.isEmpty() || normalized.length() > 200) {
            throw new DomainException(ErrorCodeConstants.USER_POOL_NAME_INVALID);
        }
        return normalized;
    }
}
