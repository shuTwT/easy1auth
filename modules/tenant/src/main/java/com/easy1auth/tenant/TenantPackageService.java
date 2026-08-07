package com.easy1auth.tenant;

import com.easy1auth.foundation.error.DomainException;
import com.easy1auth.tenant.model.TenantPackageEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TenantPackageService {
    private static final long SYSTEM_PACKAGE_ID = 0L;
    private static final String SYSTEM_PACKAGE_CODE = "system";

    private final TenantPackageRepository repository;
    private final TenantPackagePermissionCatalog permissionCatalog;

    TenantPackageService(TenantPackageRepository repository, TenantPackagePermissionCatalog permissionCatalog) {
        this.repository = repository;
        this.permissionCatalog = permissionCatalog;
    }

    @Transactional(readOnly = true)
    public List<TenantPackageView> list() {
        return repository.list().stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public TenantPackageView get(long packageId) {
        if (packageId == SYSTEM_PACKAGE_ID) {
            return systemPackage();
        }
        return view(requiredPositivePackage(packageId));
    }

    @Transactional(readOnly = true)
    public TenantPackageView getActive(long packageId) {
        positivePackageId(packageId);
        return repository.findActive(packageId).map(this::activeOrdinaryView)
                .orElseThrow(() -> new DomainException(ErrorCodeConstants.TENANT_PACKAGE_NOT_ACTIVE));
    }

    @Transactional
    public TenantPackageView lockActiveAssignable(long packageId) {
        var tenantPackage = requiredPositivePackageForUpdate(packageId);
        if (!"active".equals(tenantPackage.status())) {
            throw new DomainException(ErrorCodeConstants.TENANT_PACKAGE_NOT_ACTIVE);
        }
        return activeOrdinaryView(tenantPackage);
    }

    @Transactional
    public TenantPackageView lockActiveDefaultAssignable() {
        return repository.lockActiveDefault().map(this::activeOrdinaryView)
                .orElseThrow(() -> new DomainException(ErrorCodeConstants.TENANT_PACKAGE_DEFAULT_NOT_ACTIVE));
    }

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

    @Transactional
    public TenantPackageView replacePermissions(long packageId, List<String> permissionCodes) {
        var existing = requiredPositivePackageForUpdate(packageId);
        repository.replacePermissions(existing.id(), validatePermissionCodes(permissionCodes));
        return activeOrdinaryView(requiredPositivePackage(existing.id()));
    }

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

    private TenantPackageEntity requiredPositivePackage(long packageId) {
        positivePackageId(packageId);
        return repository.find(packageId)
                .orElseThrow(() -> new DomainException(ErrorCodeConstants.TENANT_PACKAGE_NOT_FOUND));
    }

    private TenantPackageEntity requiredPositivePackageForUpdate(long packageId) {
        positivePackageId(packageId);
        return repository.lock(packageId)
                .orElseThrow(() -> new DomainException(ErrorCodeConstants.TENANT_PACKAGE_NOT_FOUND));
    }

    private static void positivePackageId(long packageId) {
        if (packageId <= SYSTEM_PACKAGE_ID) {
            throw new DomainException(ErrorCodeConstants.TENANT_PACKAGE_ID_INVALID);
        }
    }

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

    private void ensureUnique(TenantPackageMutation mutation, Long excludingId) {
        if (repository.codeExists(mutation.code(), excludingId)) {
            throw new DomainException(ErrorCodeConstants.TENANT_PACKAGE_CODE_EXISTS);
        }
        if (repository.nameExists(mutation.name(), excludingId)) {
            throw new DomainException(ErrorCodeConstants.TENANT_PACKAGE_NAME_EXISTS);
        }
    }

    private List<String> validatePermissionCodes(List<String> permissionCodes) {
        if (permissionCodes == null || permissionCodes.stream().anyMatch(java.util.Objects::isNull)) {
            throw new DomainException(ErrorCodeConstants.TENANT_PACKAGE_PERMISSION_CODES_INVALID);
        }
        return permissionCatalog.validateActiveTenantPermissionCodes(permissionCodes);
    }

    private TenantPackageView activeOrdinaryView(TenantPackageEntity entity) {
        return view(entity);
    }

    private static String requiredText(String value, int maxLength, com.easy1auth.foundation.error.ErrorCode errorCode) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isEmpty() || normalized.length() > maxLength) {
            throw new DomainException(errorCode);
        }
        return normalized;
    }
}
