package com.easy1auth.tenant.infrastructure.repository;

import com.easy1auth.tenant.dto.UserPoolPackageMutation;
import com.easy1auth.tenant.model.UserPoolPackageEntity;
import com.easy1auth.tenant.model.UserPoolPackageEntityDraft;
import com.easy1auth.tenant.model.UserPoolPackageEntityTable;
import com.easy1auth.tenant.model.UserPoolPackagePermissionEntityDraft;
import com.easy1auth.tenant.model.UserPoolPackagePermissionEntityTable;
import com.easy1auth.tenant.model.UserPoolPackagePermissionIdDraft;
import com.easy1auth.tenant.model.UserPoolEntityTable;
import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 用户池套餐数据访问仓储（包私有）。
 *
 * <p>封装对 tenant_package / tenant_package_permission / tenant 表的读写，
 * 通过 jimmer {@link JSqlClient} 完成查询与更新；写操作相关的读取使用
 * {@code forUpdate} 行级锁保证套餐分配与配额在并发下的一致性。</p>
 */
public interface UserPoolPackageRepository extends JRepository<UserPoolPackageEntity, Long> {
    /** tenant_package 表静态描述符 */
    UserPoolPackageEntityTable PACKAGE = UserPoolPackageEntityTable.$;
    /** tenant_package_permission 表静态描述符 */
    UserPoolPackagePermissionEntityTable PACKAGE_PERMISSION = UserPoolPackagePermissionEntityTable.$;
    /** tenant 表静态描述符 */
    UserPoolEntityTable USER_POOL = UserPoolEntityTable.$;

    /** 查询全部套餐，按创建时间与 ID 倒序。 */
    default List<UserPoolPackageEntity> list() {
        return sql().createQuery(PACKAGE)
                .orderBy(PACKAGE.createdAt().desc(), PACKAGE.id().desc())
                .select(PACKAGE)
                .execute();
    }

    /** 按 ID 查询套餐。 */
    default Optional<UserPoolPackageEntity> find(long packageId) {
        return sql().createQuery(PACKAGE)
                .where(PACKAGE.id().eq(packageId))
                .select(PACKAGE)
                .fetchOptional();
    }

    /** 按 ID 加行级锁查询套餐（供写操作前的并发保护）。 */
    default Optional<UserPoolPackageEntity> lock(long packageId) {
        return sql().createQuery(PACKAGE)
                .where(PACKAGE.id().eq(packageId))
                .select(PACKAGE)
                .forUpdate()
                .fetchOptional();
    }

    /** 加行级锁查询启用中的默认套餐（供创建普通用户池时默认绑定）。 */
    default Optional<UserPoolPackageEntity> lockActiveDefault() {
        return sql().createQuery(PACKAGE)
                .where(PACKAGE.id().gt(0L), PACKAGE.defaultPackage().eq(true), PACKAGE.status().eq("active"))
                .select(PACKAGE)
                .forUpdate()
                .fetchOptional();
    }

    /** 按编码查询套餐。 */
    default Optional<UserPoolPackageEntity> findByCode(String code) {
        return sql().createQuery(PACKAGE)
                .where(PACKAGE.code().eq(code))
                .select(PACKAGE)
                .fetchOptional();
    }

    /** 按 ID 查询 active 状态的套餐。 */
    default Optional<UserPoolPackageEntity> findActive(long packageId) {
        return sql().createQuery(PACKAGE)
                .where(PACKAGE.id().eq(packageId), PACKAGE.status().eq("active"))
                .select(PACKAGE)
                .fetchOptional();
    }

    /** 判断编码是否已被使用（可排除指定 ID，用于更新场景）。 */
    default boolean codeExists(String code, Long excludingId) {
        return sql().createQuery(PACKAGE)
                .where(PACKAGE.code().eq(code))
                .whereIf(excludingId != null, () -> PACKAGE.id().ne(excludingId))
                .select(PACKAGE.id())
                .exists();
    }

    /** 判断名称是否已被使用（可排除指定 ID，用于更新场景）。 */
    default boolean nameExists(String name, Long excludingId) {
        return sql().createQuery(PACKAGE)
                .where(PACKAGE.name().eq(name))
                .whereIf(excludingId != null, () -> PACKAGE.id().ne(excludingId))
                .select(PACKAGE.id())
                .exists();
    }

    /** 判断套餐是否已被普通用户池绑定（用于停用/删除前的保护校验）。 */
    default boolean isReferencedByOrdinaryUserPool(long packageId) {
        return sql().createQuery(USER_POOL)
                .where(USER_POOL.system().eq(false), USER_POOL.packageInfo().id().eq(packageId))
                .select(USER_POOL.id())
                .exists();
    }

    /** 新建套餐（status=active）。 */
    default void create(UserPoolPackageMutation mutation) {
        Instant now = Instant.now();
        sql().saveCommand(UserPoolPackageEntityDraft.$.produce(draft -> draft
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

    /** 更新套餐的编码、名称、默认标记与配额。 */
    default void update(long packageId, UserPoolPackageMutation mutation) {
        sql().createUpdate(PACKAGE)
                .set(PACKAGE.code(), mutation.code())
                .set(PACKAGE.name(), mutation.name())
                .set(PACKAGE.defaultPackage(), mutation.defaultPackage())
                .set(PACKAGE.maxUsers(), mutation.maxUsers())
                .set(PACKAGE.maxApps(), mutation.maxApps())
                .set(PACKAGE.updatedAt(), Instant.now())
                .where(PACKAGE.id().eq(packageId))
                .execute();
    }

    /** 更新套餐状态（active / inactive）。 */
    default void updateStatus(long packageId, String status) {
        sql().createUpdate(PACKAGE)
                .set(PACKAGE.status(), status)
                .set(PACKAGE.updatedAt(), Instant.now())
                .where(PACKAGE.id().eq(packageId))
                .execute();
    }

    /** 删除套餐（调用方须先完成默认/被绑定保护校验）。 */
    default void delete(long packageId) {
        sql().createDelete(PACKAGE)
                .where(PACKAGE.id().eq(packageId))
                .execute();
    }

    /** 查询套餐绑定的权限编码，按编码升序返回。 */
    default List<String> permissionCodes(long packageId) {
        return sql().createQuery(PACKAGE_PERMISSION)
                .where(PACKAGE_PERMISSION.id().packageId().eq(packageId))
                .orderBy(PACKAGE_PERMISSION.id().permissionCode().asc())
                .select(PACKAGE_PERMISSION.id().permissionCode())
                .execute();
    }

    /** 整体替换套餐的权限列表：先删除原有关联，再批量写入。 */
    default void replacePermissions(long packageId, Collection<String> permissionCodes) {
        sql().createDelete(PACKAGE_PERMISSION)
                .where(PACKAGE_PERMISSION.id().packageId().eq(packageId))
                .execute();
        Instant now = Instant.now();
        permissionCodes.forEach(permissionCode -> sql().saveCommand(
                        UserPoolPackagePermissionEntityDraft.$.produce(draft -> draft
                                .setId(UserPoolPackagePermissionIdDraft.$.produce(id -> id
                                        .setPackageId(packageId)
                                        .setPermissionCode(permissionCode)))
                                .setCreatedAt(now)))
                .setMode(SaveMode.INSERT_ONLY)
                .execute());
    }
}
