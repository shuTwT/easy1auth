package com.easy1auth.adminaccess;

import com.easy1auth.adminaccess.model.ManagementPermissionEntity;
import com.easy1auth.adminaccess.model.ManagementPermissionEntityTable;
import com.easy1auth.foundation.error.DomainException;
import org.babyfish.jimmer.sql.JSqlClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ManagementPermissionCatalog {
    private static final ManagementPermissionEntityTable PERMISSION = ManagementPermissionEntityTable.$;

    private final JSqlClient sql;

    public ManagementPermissionCatalog(JSqlClient sql) {
        this.sql = sql;
    }

    @Transactional(readOnly = true)
    public List<ManagementPermissionView> activeViews() {
        return sql.createQuery(PERMISSION)
                .where(PERMISSION.active().eq(true))
                .orderBy(PERMISSION.sortOrder().asc(), PERMISSION.code().asc())
                .select(PERMISSION)
                .execute()
                .stream()
                .map(this::view)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ManagementPermissionView> activeViews(ManagementPermissionScope scope) {
        return activeViews().stream().filter(view -> view.scope() == scope).toList();
    }

    @Transactional(readOnly = true)
    public List<ManagementPermissionCode> validate(Collection<String> rawCodes, ManagementPermissionScope scope) {
        if (rawCodes == null) {
            throw invalidCode();
        }
        var codes = rawCodes.stream().map(this::code).toList();
        if (codes.isEmpty()) {
            return List.of();
        }
        if (codes.size() != codes.stream().distinct().count()) {
            throw invalidCode();
        }
        if (codes.stream().anyMatch(code -> code.scope() != scope)) {
            throw wrongScope();
        }
        var rows = sql.createQuery(PERMISSION)
                .where(PERMISSION.code().in(codes.stream().map(ManagementPermissionCode::value).toList()))
                .select(PERMISSION)
                .execute();
        Map<String, ManagementPermissionEntity> rowsByCode = rows.stream().collect(
                LinkedHashMap::new,
                (result, row) -> result.put(row.code(), row),
                Map::putAll);
        codes.forEach(code -> validateRow(code, rowsByCode.get(code.value())));
        return codes;
    }

    @Transactional(readOnly = true)
    public List<ManagementPermissionCode> activeCodes(ManagementPermissionScope scope) {
        return activeViews(scope).stream().map(view -> code(view.code())).toList();
    }

    private ManagementPermissionView view(ManagementPermissionEntity entity) {
        var code = code(entity.code());
        var type = ManagementPermissionType.fromDatabaseValue(entity.type()).orElseThrow(this::invalidMetadata);
        if (code.type() != type) {
            throw invalidMetadata();
        }
        return new ManagementPermissionView(
                code.value(), type, code.scope(), entity.name(), entity.parentCode(), entity.resource(),
                entity.action(), entity.sortOrder(), entity.active());
    }

    private void validateRow(ManagementPermissionCode code, ManagementPermissionEntity entity) {
        if (entity == null) {
            throw invalidMetadata();
        }
        if (!entity.active()) {
            throw inactive();
        }
        if (ManagementPermissionType.fromDatabaseValue(entity.type()).orElseThrow(this::invalidMetadata) != code.type()) {
            throw invalidMetadata();
        }
    }

    private ManagementPermissionCode code(String value) {
        return ManagementPermissionCode.fromValue(value).orElseThrow(this::invalidCode);
    }

    private DomainException invalidCode() {
        return new DomainException("MANAGEMENT_PERMISSION_INVALID", "包含未知或非管理端权限", 400);
    }

    private DomainException wrongScope() {
        return new DomainException("MANAGEMENT_PERMISSION_SCOPE_INVALID", "权限不属于当前作用域", 400);
    }

    private DomainException inactive() {
        return new DomainException("MANAGEMENT_PERMISSION_INACTIVE", "权限已停用", 400);
    }

    private DomainException invalidMetadata() {
        return new DomainException("MANAGEMENT_PERMISSION_METADATA_INVALID", "权限目录元数据无效", 409);
    }
}
