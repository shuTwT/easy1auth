package com.easy1auth.adminaccess;

import com.easy1auth.adminaccess.constant.ErrorCodeConstants;
import com.easy1auth.adminaccess.constant.ManagementPermissionCode;
import com.easy1auth.adminaccess.constant.ManagementPermissionScope;
import com.easy1auth.adminaccess.constant.ManagementPermissionType;
import com.easy1auth.adminaccess.dto.ManagementPermissionView;
import com.easy1auth.adminaccess.model.ManagementPermissionEntity;
import com.easy1auth.adminaccess.model.ManagementPermissionEntityTable;
import com.easy1auth.common.foundation.error.DomainException;
import org.babyfish.jimmer.sql.JSqlClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理端权限目录服务。
 *
 * <p>以 management_permission 表为数据源，提供权限视图查询、权限码校验与
 * 作用域过滤能力。校验时要求权限存在、启用且作用域匹配，并核对枚举类型与
 * 数据库存储值的一致性，异常情况抛出对应的领域异常。</p>
 */
@Service
public class ManagementPermissionCatalog {
    /** management_permission 表静态描述符 */
    private static final ManagementPermissionEntityTable PERMISSION = ManagementPermissionEntityTable.$;

    /** jimmer SQL 客户端 */
    private final JSqlClient sql;

    public ManagementPermissionCatalog(JSqlClient sql) {
        this.sql = sql;
    }

    /** 查询全部启用权限的视图列表（按排序号、权限码升序）。 */
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

    /** 查询指定作用域下全部启用权限的视图列表。 */
    @Transactional(readOnly = true)
    public List<ManagementPermissionView> activeViews(ManagementPermissionScope scope) {
        return activeViews().stream().filter(view -> view.scope() == scope).toList();
    }

    /** 校验一组权限码：均须已知、启用且作用域匹配，返回规范化后的权限码列表。 */
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

    /** 返回指定作用域下全部启用权限码。 */
    @Transactional(readOnly = true)
    public List<ManagementPermissionCode> activeCodes(ManagementPermissionScope scope) {
        return activeViews(scope).stream().map(view -> code(view.code())).toList();
    }

    /** 将权限实体转换为视图，并校验枚举与数据库存储的类型一致。 */
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

    /** 校验单条权限记录：存在、启用且类型与枚举一致，否则抛出对应领域异常。 */
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

    /** 按权限码字符串反查枚举，未知时抛权限无效异常。 */
    private ManagementPermissionCode code(String value) {
        return ManagementPermissionCode.fromValue(value).orElseThrow(this::invalidCode);
    }

    /** 构造权限无效的领域异常。 */
    private DomainException invalidCode() {
        return new DomainException(ErrorCodeConstants.MANAGEMENT_PERMISSION_INVALID);
    }

    /** 构造作用域不匹配的领域异常。 */
    private DomainException wrongScope() {
        return new DomainException(ErrorCodeConstants.MANAGEMENT_PERMISSION_SCOPE_INVALID);
    }

    /** 构造权限已停用的领域异常。 */
    private DomainException inactive() {
        return new DomainException(ErrorCodeConstants.MANAGEMENT_PERMISSION_INACTIVE);
    }

    /** 构造权限目录元数据无效的领域异常。 */
    private DomainException invalidMetadata() {
        return new DomainException(ErrorCodeConstants.MANAGEMENT_PERMISSION_METADATA_INVALID);
    }
}
