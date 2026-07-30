package com.easy1auth.tenant;

import com.easy1auth.tenant.model.TenantPackageEntity;
import com.easy1auth.tenant.model.TenantPackageEntityDraft;
import com.easy1auth.tenant.model.TenantPackageEntityTable;
import com.easy1auth.tenant.model.TenantPackagePermissionEntityDraft;
import com.easy1auth.tenant.model.TenantPackagePermissionEntityTable;
import com.easy1auth.tenant.model.TenantPackagePermissionIdDraft;
import com.easy1auth.tenant.model.TenantEntityTable;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
class TenantPackageRepository {
    private static final TenantPackageEntityTable PACKAGE = TenantPackageEntityTable.$;
    private static final TenantPackagePermissionEntityTable PACKAGE_PERMISSION = TenantPackagePermissionEntityTable.$;
    private static final TenantEntityTable TENANT = TenantEntityTable.$;

    private final JSqlClient sql;

    TenantPackageRepository(JSqlClient sql) {
        this.sql = sql;
    }

    List<TenantPackageEntity> list() {
        return sql.createQuery(PACKAGE)
                .orderBy(PACKAGE.createdAt().desc(), PACKAGE.id().desc())
                .select(PACKAGE)
                .execute();
    }

    Optional<TenantPackageEntity> find(long packageId) {
        return sql.createQuery(PACKAGE)
                .where(PACKAGE.id().eq(packageId))
                .select(PACKAGE)
                .fetchOptional();
    }

    Optional<TenantPackageEntity> lock(long packageId) {
        return sql.createQuery(PACKAGE)
                .where(PACKAGE.id().eq(packageId))
                .select(PACKAGE)
                .forUpdate()
                .fetchOptional();
    }

    Optional<TenantPackageEntity> lockActiveDefault() {
        return sql.createQuery(PACKAGE)
                .where(PACKAGE.id().gt(0L), PACKAGE.defaultPackage().eq(true), PACKAGE.status().eq("active"))
                .select(PACKAGE)
                .forUpdate()
                .fetchOptional();
    }

    Optional<TenantPackageEntity> findByCode(String code) {
        return sql.createQuery(PACKAGE)
                .where(PACKAGE.code().eq(code))
                .select(PACKAGE)
                .fetchOptional();
    }

    Optional<TenantPackageEntity> findActive(long packageId) {
        return sql.createQuery(PACKAGE)
                .where(PACKAGE.id().eq(packageId), PACKAGE.status().eq("active"))
                .select(PACKAGE)
                .fetchOptional();
    }

    boolean codeExists(String code, Long excludingId) {
        return sql.createQuery(PACKAGE)
                .where(PACKAGE.code().eq(code))
                .whereIf(excludingId != null, () -> PACKAGE.id().ne(excludingId))
                .select(PACKAGE.id())
                .exists();
    }

    boolean nameExists(String name, Long excludingId) {
        return sql.createQuery(PACKAGE)
                .where(PACKAGE.name().eq(name))
                .whereIf(excludingId != null, () -> PACKAGE.id().ne(excludingId))
                .select(PACKAGE.id())
                .exists();
    }

    boolean isReferencedByOrdinaryTenant(long packageId) {
        return sql.createQuery(TENANT)
                .where(TENANT.system().eq(false), TENANT.packageInfo().id().eq(packageId))
                .select(TENANT.id())
                .exists();
    }

    void create(TenantPackageMutation mutation) {
        Instant now = Instant.now();
        sql.saveCommand(TenantPackageEntityDraft.$.produce(draft -> draft
                        .setCode(mutation.code())
                        .setName(mutation.name())
                        .setStatus("active")
                        .setDefaultPackage(mutation.defaultPackage())
                        .setMaxUsers(mutation.maxUsers())
                        .setMaxApps(mutation.maxApps())
                        .setCreatedAt(now)
                        .setUpdatedAt(now)))
                .setMode(SaveMode.INSERT_ONLY)
                .execute();
    }

    void update(long packageId, TenantPackageMutation mutation) {
        sql.createUpdate(PACKAGE)
                .set(PACKAGE.code(), mutation.code())
                .set(PACKAGE.name(), mutation.name())
                .set(PACKAGE.defaultPackage(), mutation.defaultPackage())
                .set(PACKAGE.maxUsers(), mutation.maxUsers())
                .set(PACKAGE.maxApps(), mutation.maxApps())
                .set(PACKAGE.updatedAt(), Instant.now())
                .where(PACKAGE.id().eq(packageId))
                .execute();
    }

    void updateStatus(long packageId, String status) {
        sql.createUpdate(PACKAGE)
                .set(PACKAGE.status(), status)
                .set(PACKAGE.updatedAt(), Instant.now())
                .where(PACKAGE.id().eq(packageId))
                .execute();
    }

    void delete(long packageId) {
        sql.createDelete(PACKAGE)
                .where(PACKAGE.id().eq(packageId))
                .execute();
    }

    List<String> permissionCodes(long packageId) {
        return sql.createQuery(PACKAGE_PERMISSION)
                .where(PACKAGE_PERMISSION.id().packageId().eq(packageId))
                .orderBy(PACKAGE_PERMISSION.id().permissionCode().asc())
                .select(PACKAGE_PERMISSION.id().permissionCode())
                .execute();
    }

    void replacePermissions(long packageId, Collection<String> permissionCodes) {
        sql.createDelete(PACKAGE_PERMISSION)
                .where(PACKAGE_PERMISSION.id().packageId().eq(packageId))
                .execute();
        Instant now = Instant.now();
        permissionCodes.forEach(permissionCode -> sql.saveCommand(
                        TenantPackagePermissionEntityDraft.$.produce(draft -> draft
                                .setId(TenantPackagePermissionIdDraft.$.produce(id -> id
                                        .setPackageId(packageId)
                                        .setPermissionCode(permissionCode)))
                                .setCreatedAt(now)))
                .setMode(SaveMode.INSERT_ONLY)
                .execute());
    }
}
