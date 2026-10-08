package com.easy1auth.tenant.service;

import com.easy1auth.framework.common.error.DomainException;
import com.easy1auth.framework.common.error.ErrorCode;
import com.easy1auth.framework.common.id.UuidV7;
import com.easy1auth.framework.common.pagination.PageData;
import com.easy1auth.tenant.dto.TenantInput;
import com.easy1auth.tenant.dto.TenantView;
import com.easy1auth.tenant.model.TenantEntity;
import com.easy1auth.tenant.model.TenantEntityDraft;
import com.easy1auth.tenant.model.TenantEntityTable;
import com.easy1auth.tenant.model.UserPoolEntityTable;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.LikeMode;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/** 用户池下的租户资料管理。租户状态只影响资料状态，不改变后台或授权登录行为。 */
@Service
public class TenantService {
    private static final TenantEntityTable TENANT = TenantEntityTable.$;
    private static final UserPoolEntityTable POOL = UserPoolEntityTable.$;
    private static final ErrorCode NOT_FOUND = new ErrorCode(11101, "当前用户池下的租户不存在");
    private static final ErrorCode INVALID_NAME = new ErrorCode(11102, "租户名称不能为空且不能超过200个字符");
    private static final ErrorCode INVALID_CODE = new ErrorCode(11103, "租户编码须为1至100位小写字母、数字、下划线或连字符");
    private static final ErrorCode CODE_EXISTS = new ErrorCode(11104, "当前用户池下的租户编码已存在");
    private static final ErrorCode INVALID_STATUS = new ErrorCode(11105, "租户状态只能是 active 或 inactive");
    private static final ErrorCode INVALID_DESCRIPTION = new ErrorCode(11106, "租户描述不能超过2000个字符");
    private static final ErrorCode POOL_UNAVAILABLE = new ErrorCode(11107, "用户池不存在或未启用");

    private final JSqlClient sql;

    public TenantService(JSqlClient sql) {
        this.sql = sql;
    }

    @Transactional(readOnly = true)
    public PageData<TenantView> list(UUID userPoolId, int page, int pageSize, String name, String status) {
        int normalizedPage = Math.max(1, page);
        int size = Math.min(100, Math.max(1, pageSize));
        var query = sql.createQuery(TENANT)
                .where(TENANT.userPoolId().eq(userPoolId))
                .whereIf(name != null && !name.isBlank(), () -> TENANT.name().ilike(name.strip(), LikeMode.ANYWHERE))
                .whereIf(status != null && !status.isBlank(), () -> TENANT.status().eq(normalizeStatus(status)))
                .orderBy(TENANT.createdAt().desc(), TENANT.id().desc())
                .select(TENANT);
        long total = query.fetchUnlimitedCount();
        var items = query.limit(size, (long) (normalizedPage - 1) * size).execute().stream()
                .map(TenantService::view)
                .toList();
        return PageData.of(items, normalizedPage, size, total);
    }

    @Transactional(readOnly = true)
    public TenantView get(UUID userPoolId, UUID id) {
        return view(required(userPoolId, id));
    }

    @Transactional
    public TenantView create(UUID userPoolId, TenantInput input) {
        lockPool(userPoolId);
        TenantInput normalized = normalize(input);
        requireUniqueCode(userPoolId, normalized.code(), null);
        Instant now = Instant.now();
        var entity = TenantEntityDraft.$.produce(draft -> {
            draft.setId(UuidV7.randomUuid());
            draft.setUserPoolId(userPoolId);
            draft.setName(normalized.name());
            draft.setCode(normalized.code());
            draft.setDescription(normalized.description());
            draft.setStatus(normalized.status());
            draft.setCreatedAt(now);
            draft.setUpdatedAt(now);
        });
        sql.saveCommand(entity).setMode(SaveMode.INSERT_ONLY).execute();
        return view(entity);
    }

    @Transactional
    public TenantView update(UUID userPoolId, UUID id, TenantInput input) {
        lockPool(userPoolId);
        required(userPoolId, id);
        TenantInput normalized = normalize(input);
        requireUniqueCode(userPoolId, normalized.code(), id);
        sql.createUpdate(TENANT)
                .set(TENANT.name(), normalized.name())
                .set(TENANT.code(), normalized.code())
                .set(TENANT.description(), normalized.description())
                .set(TENANT.status(), normalized.status())
                .set(TENANT.updatedAt(), Instant.now())
                .where(TENANT.id().eq(id), TENANT.userPoolId().eq(userPoolId))
                .execute();
        return get(userPoolId, id);
    }

    @Transactional
    public void delete(UUID userPoolId, UUID id) {
        lockPool(userPoolId);
        required(userPoolId, id);
        sql.createDelete(TENANT)
                .where(TENANT.id().eq(id), TENANT.userPoolId().eq(userPoolId))
                .execute();
    }

    private TenantEntity required(UUID userPoolId, UUID id) {
        return sql.createQuery(TENANT)
                .where(TENANT.id().eq(id), TENANT.userPoolId().eq(userPoolId))
                .select(TENANT)
                .fetchOptional()
                .orElseThrow(() -> new DomainException(NOT_FOUND));
    }

    /** 同一用户池的写入串行化，保证编码检查与写入一致，数据库唯一约束作为最终保护。 */
    private void lockPool(UUID userPoolId) {
        sql.createQuery(POOL)
                .where(POOL.id().eq(userPoolId), POOL.status().eq("active"))
                .select(POOL.id())
                .forUpdate()
                .fetchOptional()
                .orElseThrow(() -> new DomainException(POOL_UNAVAILABLE));
    }

    private void requireUniqueCode(UUID userPoolId, String code, UUID excludedId) {
        boolean exists = sql.createQuery(TENANT)
                .where(TENANT.userPoolId().eq(userPoolId), TENANT.code().eq(code))
                .whereIf(excludedId != null, () -> TENANT.id().ne(excludedId))
                .select(TENANT.id())
                .exists();
        if (exists) {
            throw new DomainException(CODE_EXISTS);
        }
    }

    private static TenantInput normalize(TenantInput input) {
        String name = input == null || input.name() == null ? "" : input.name().strip();
        if (name.isEmpty() || name.length() > 200) {
            throw new DomainException(INVALID_NAME);
        }
        String code = input.code() == null ? "" : input.code().strip().toLowerCase(Locale.ROOT);
        if (!code.matches("[a-z0-9_-]{1,100}")) {
            throw new DomainException(INVALID_CODE);
        }
        String description = input.description() == null ? null : input.description().strip();
        if (description != null && description.length() > 2000) {
            throw new DomainException(INVALID_DESCRIPTION);
        }
        return new TenantInput(name, code, description, normalizeStatus(input.status() == null ? "active" : input.status()));
    }

    private static String normalizeStatus(String status) {
        String value = status.strip();
        if (!"active".equals(value) && !"inactive".equals(value)) {
            throw new DomainException(INVALID_STATUS);
        }
        return value;
    }

    private static TenantView view(TenantEntity entity) {
        return new TenantView(entity.id(), entity.userPoolId(), entity.name(), entity.code(), entity.description(),
                entity.status(), entity.createdAt(), entity.updatedAt());
    }
}
