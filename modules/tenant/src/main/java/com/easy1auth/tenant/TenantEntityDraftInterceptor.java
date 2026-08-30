package com.easy1auth.tenant;

import com.easy1auth.infrastructure.foundation.error.DomainException;
import com.easy1auth.infrastructure.persistence.model.BaseTenantEntity;
import com.easy1auth.infrastructure.persistence.model.BaseTenantEntityDraft;
import com.easy1auth.infrastructure.persistence.model.BaseTenantEntityProps;
import com.easy1auth.tenant.constant.ErrorCodeConstants;
import com.easy1auth.tenant.util.TenantContextHolder;
import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.DraftInterceptor;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * 租户实体草稿拦截器。
 *
 * <p>实现 jimmer 的 {@link DraftInterceptor}：在保存所有继承
 * {@link BaseTenantEntity} 的实体时，自动为草稿填充当前线程的租户 ID，
 * 并校验数据与当前租户上下文一致，从写入侧保证多租户数据隔离。</p>
 */
@Component
public final class TenantEntityDraftInterceptor
        implements DraftInterceptor<BaseTenantEntity, BaseTenantEntityDraft> {

    /**
     * 保存前自动填充并校验租户 ID：忽略标志开启或当前无租户上下文时直接放行；
     * 若数据已携带与当前租户不一致的租户 ID，则抛出领域异常阻止写入。
     */
    @Override
    public void beforeSave(BaseTenantEntityDraft draft, @Nullable BaseTenantEntity original) {
        if (TenantContextHolder.isIgnore()) {
            return;
        }
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            return;
        }
        if (original != null && !tenantId.equals(original.tenantId())) {
            throw mismatch();
        }
        if (ImmutableObjects.isLoaded(draft, BaseTenantEntityProps.TENANT_ID)) {
            if (!tenantId.equals(draft.tenantId())) {
                throw mismatch();
            }
        } else {
            draft.setTenantId(tenantId);
        }
    }

    /** 声明本拦截器关心的属性（tenantId），jimmer 据此决定何时触发回调。 */
    @Override
    public Collection<TypedProp<BaseTenantEntity, ?>> dependencies() {
        return List.of(BaseTenantEntityProps.TENANT_ID);
    }

    /** 构造“数据租户与当前租户不一致”的领域异常。 */
    private static DomainException mismatch() {
        return new DomainException(ErrorCodeConstants.TENANT_CONTEXT_MISMATCH);
    }
}
