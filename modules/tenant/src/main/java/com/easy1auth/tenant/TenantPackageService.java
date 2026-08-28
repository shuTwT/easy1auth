package com.easy1auth.tenant;

import com.easy1auth.foundation.error.DomainException;
import com.easy1auth.tenant.model.TenantPackageEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 租户套餐服务：套餐生命周期管理（创建、更新、状态变更、权限替换、删除）与查询，
 * 以及供租户绑定、配额校验使用的并发锁定方法。
 *
 * <p>套餐是普通租户配额与权限的依据，写操作均处于事务边界内，
 * 并对目标套餐加行级锁以保证配额与分配的一致性。</p>
 */
@Service
public class TenantPackageService {
    /** 系统套餐的固定 ID（0，不落库，由系统套餐方法在代码中构造） */
    private static final long SYSTEM_PACKAGE_ID = 0L;
    /** 系统套餐的固定编码 */
    private static final String SYSTEM_PACKAGE_CODE = "system";

    /** 租户套餐数据访问仓储 */
    private final TenantPackageRepository repository;
    /** 套餐权限目录（校验租户套餐可用权限、查询系统套餐平台权限） */
    private final TenantPackagePermissionCatalog permissionCatalog;

    TenantPackageService(TenantPackageRepository repository, TenantPackagePermissionCatalog permissionCatalog) {
        this.repository = repository;
        this.permissionCatalog = permissionCatalog;
    }

    /** 查询全部租户套餐，按创建时间倒序。 */
    @Transactional(readOnly = true)
    public List<TenantPackageView> list() {
        return repository.list().stream().map(this::view).toList();
    }

    /** 查询指定套餐；ID 为 0 时返回内置的系统套餐。 */
    @Transactional(readOnly = true)
    public TenantPackageView get(long packageId) {
        if (packageId == SYSTEM_PACKAGE_ID) {
            return systemPackage();
        }
        return view(requiredPositivePackage(packageId));
    }

    /** 查询处于 active 状态的套餐（用于只读的分配选择），不存在或未启用时抛出领域异常。 */
    @Transactional(readOnly = true)
    public TenantPackageView getActive(long packageId) {
        positivePackageId(packageId);
        return repository.findActive(packageId).map(this::activeOrdinaryView)
                .orElseThrow(() -> new DomainException(ErrorCodeConstants.TENANT_PACKAGE_NOT_ACTIVE));
    }

    /** 加行级锁校验套餐为 active 后返回（供普通租户绑定套餐前的并发保护）。 */
    @Transactional
    public TenantPackageView lockActiveAssignable(long packageId) {
        var tenantPackage = requiredPositivePackageForUpdate(packageId);
        if (!"active".equals(tenantPackage.status())) {
            throw new DomainException(ErrorCodeConstants.TENANT_PACKAGE_NOT_ACTIVE);
        }
        return activeOrdinaryView(tenantPackage);
    }

    /** 加行级锁查询启用中的默认套餐（供创建租户时默认绑定）。 */
    @Transactional
    public TenantPackageView lockActiveDefaultAssignable() {
        return repository.lockActiveDefault().map(this::activeOrdinaryView)
                .orElseThrow(() -> new DomainException(ErrorCodeConstants.TENANT_PACKAGE_DEFAULT_NOT_ACTIVE));
    }

    /** 创建租户套餐（status=active）并写入其权限列表。 */
    @Transactional
    public TenantPackageView create(TenantPackageMutation mutation) {
        var normalized = normalize(mutation);
        ensureUnique(normalized, null);
        var permissionCodes = validatePermissionCodes(normalized.permissionCodes());
        repository.create(normalized);
        var created = repository.findByCode(normalized.code())
                .orElseThrow(() -> new IllegalStateException("created tenant package missing"));
        repository.replacePermissions(created.id(), permissionCodes);
        return view(created);
    }

    /** 更新套餐编码/名称/配额并替换权限列表（默认标记沿用现有值，不可经此修改）。 */
    @Transactional
    public TenantPackageView update(long packageId, TenantPackageMutation mutation) {
        var existing = requiredPositivePackageForUpdate(packageId);
        var requested = normalize(mutation);
        var normalized = new TenantPackageMutation(
                requested.code(),
                requested.name(),
                existing.defaultPackage(),
                requested.maxUsers(),
                requested.maxApps(),
                requested.permissionCodes());
        ensureUnique(normalized, existing.id());
        var permissionCodes = validatePermissionCodes(normalized.permissionCodes());
        repository.update(existing.id(), normalized);
        repository.replacePermissions(existing.id(), permissionCodes);
        return view(requiredPositivePackage(existing.id()));
    }

    /**
     * 更新套餐状态（active / inactive）。
     *
     * <p>默认套餐不允许停用；停用已被普通租户绑定的套餐会被拒绝。
     * 恢复为 active 时会校验套餐配额仍可用于分配。</p>
     */
    @Transactional
    public TenantPackageView updateStatus(long packageId, String status) {
        var existing = requiredPositivePackageForUpdate(packageId);
        String normalized = status == null ? "" : status.strip();
        if (!"active".equals(normalized) && !"inactive".equals(normalized)) {
            throw new DomainException(ErrorCodeConstants.TENANT_PACKAGE_STATUS_INVALID);
        }
        if (existing.defaultPackage() && !"active".equals(normalized)) {
            throw new DomainException(ErrorCodeConstants.TENANT_PACKAGE_DEFAULT_MUST_BE_ACTIVE);
        }
        if ("active".equals(normalized)) {
            activeOrdinaryView(existing);
        }
        if ("inactive".equals(normalized) && repository.isReferencedByOrdinaryTenant(existing.id())) {
            throw new DomainException(ErrorCodeConstants.TENANT_PACKAGE_ASSIGNED_SUSPEND);
        }
        repository.updateStatus(existing.id(), normalized);
        return view(requiredPositivePackage(existing.id()));
    }

    /** 替换套餐的权限编码列表（先校验再整体覆盖）。 */
    @Transactional
    public TenantPackageView replacePermissions(long packageId, List<String> permissionCodes) {
        var existing = requiredPositivePackageForUpdate(packageId);
        repository.replacePermissions(existing.id(), validatePermissionCodes(permissionCodes));
        return activeOrdinaryView(requiredPositivePackage(existing.id()));
    }

    /** 删除套餐（默认套餐与已被普通租户绑定的套餐不允许删除）。 */
    @Transactional
    public void delete(long packageId) {
        var existing = requiredPositivePackageForUpdate(packageId);
        if (existing.defaultPackage()) {
            throw new DomainException(ErrorCodeConstants.TENANT_PACKAGE_DEFAULT_DELETE_FORBIDDEN);
        }
        if (repository.isReferencedByOrdinaryTenant(existing.id())) {
            throw new DomainException(ErrorCodeConstants.TENANT_PACKAGE_ASSIGNED_DELETE);
        }
        repository.delete(existing.id());
    }

    /** 构造并返回内置系统套餐视图（ID 0、无限配额、全部平台权限、不落库）。 */
    public TenantPackageView systemPackage() {
        return new TenantPackageView(
                SYSTEM_PACKAGE_ID,
                SYSTEM_PACKAGE_CODE,
                "系统套餐",
                "active",
                false,
                Integer.MAX_VALUE,
                Integer.MAX_VALUE,
                permissionCatalog.activePlatformPermissionCodes(),
                null,
                null);
    }

    /** 将套餐实体转换为套餐视图（含权限编码列表）。 */
    TenantPackageView view(TenantPackageEntity entity) {
        return new TenantPackageView(
                entity.id(),
                entity.code(),
                entity.name(),
                entity.status(),
                entity.defaultPackage(),
                entity.maxUsers(),
                entity.maxApps(),
                repository.permissionCodes(entity.id()),
                entity.createdAt(),
                entity.updatedAt());
    }

    /** 按 ID 查询套餐，不存在时抛出领域异常（不加锁）。 */
    private TenantPackageEntity requiredPositivePackage(long packageId) {
        positivePackageId(packageId);
        return repository.find(packageId)
                .orElseThrow(() -> new DomainException(ErrorCodeConstants.TENANT_PACKAGE_NOT_FOUND));
    }

    /** 按 ID 加行级锁查询套餐（供写操作使用），不存在时抛出领域异常。 */
    private TenantPackageEntity requiredPositivePackageForUpdate(long packageId) {
        positivePackageId(packageId);
        return repository.lock(packageId)
                .orElseThrow(() -> new DomainException(ErrorCodeConstants.TENANT_PACKAGE_NOT_FOUND));
    }

    /** 校验套餐 ID 必须为正数（0 为系统套餐，不参与此类校验）。 */
    private static void positivePackageId(long packageId) {
        if (packageId <= SYSTEM_PACKAGE_ID) {
            throw new DomainException(ErrorCodeConstants.TENANT_PACKAGE_ID_INVALID);
        }
    }

    /** 规范化并校验套餐变更数据：编码/名称非空且不超过 100 字符，配额必须为正数。 */
    private static TenantPackageMutation normalize(TenantPackageMutation mutation) {
        if (mutation == null) {
            throw new DomainException(ErrorCodeConstants.TENANT_PACKAGE_REQUIRED);
        }
        String code = requiredText(mutation.code(), 100, ErrorCodeConstants.TENANT_PACKAGE_CODE_INVALID);
        String name = requiredText(mutation.name(), 100, ErrorCodeConstants.TENANT_PACKAGE_NAME_INVALID);
        if (mutation.maxUsers() <= 0 || mutation.maxApps() <= 0) {
            throw new DomainException(ErrorCodeConstants.TENANT_PACKAGE_QUOTA_INVALID);
        }
        return new TenantPackageMutation(code, name, mutation.defaultPackage(), mutation.maxUsers(), mutation.maxApps(), mutation.permissionCodes());
    }

    /** 校验套餐编码与名称在库中唯一（可排除指定 ID，用于更新场景）。 */
    private void ensureUnique(TenantPackageMutation mutation, Long excludingId) {
        if (repository.codeExists(mutation.code(), excludingId)) {
            throw new DomainException(ErrorCodeConstants.TENANT_PACKAGE_CODE_EXISTS);
        }
        if (repository.nameExists(mutation.name(), excludingId)) {
            throw new DomainException(ErrorCodeConstants.TENANT_PACKAGE_NAME_EXISTS);
        }
    }

    /** 校验权限编码列表非空且不含空元素，并交由权限目录校验后返回。 */
    private List<String> validatePermissionCodes(List<String> permissionCodes) {
        if (permissionCodes == null || permissionCodes.stream().anyMatch(java.util.Objects::isNull)) {
            throw new DomainException(ErrorCodeConstants.TENANT_PACKAGE_PERMISSION_CODES_INVALID);
        }
        return permissionCatalog.validateActiveTenantPermissionCodes(permissionCodes);
    }

    /** 将套餐实体转换为套餐视图（普通套餐分配场景）。 */
    private TenantPackageView activeOrdinaryView(TenantPackageEntity entity) {
        return view(entity);
    }

    /** 校验并返回去除首尾空白的文本：空或超过 maxLength 时抛出指定领域异常。 */
    private static String requiredText(String value, int maxLength, com.easy1auth.foundation.error.ErrorCode errorCode) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isEmpty() || normalized.length() > maxLength) {
            throw new DomainException(errorCode);
        }
        return normalized;
    }
}
