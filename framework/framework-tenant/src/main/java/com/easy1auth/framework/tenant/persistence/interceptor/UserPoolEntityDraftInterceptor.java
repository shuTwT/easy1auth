package com.easy1auth.framework.tenant.persistence.interceptor;

import com.easy1auth.framework.common.error.DomainException;
import com.easy1auth.framework.tenant.persistence.model.BaseUserPoolEntity;
import com.easy1auth.framework.tenant.persistence.model.BaseUserPoolEntityDraft;
import com.easy1auth.framework.tenant.persistence.model.BaseUserPoolEntityProps;
import com.easy1auth.framework.tenant.error.ErrorCodeConstants;
import com.easy1auth.framework.tenant.context.UserPoolContextHolder;
import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.DraftInterceptor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * 用户池实体草稿拦截器。
 *
 * <p>实现 jimmer 的 {@link DraftInterceptor}：在保存所有继承
 * {@link BaseUserPoolEntity} 的实体时，自动为草稿填充当前线程的用户池 ID，
 * 并校验数据与当前用户池上下文一致，从写入侧保证多用户池数据隔离。</p>
 */
@Component
public final class UserPoolEntityDraftInterceptor
        implements DraftInterceptor<BaseUserPoolEntity, BaseUserPoolEntityDraft> {

    /**
     * 保存前自动填充并校验用户池 ID：忽略标志开启或当前无用户池上下文时直接放行；
     * 若数据已携带与当前用户池不一致的用户池 ID，则抛出领域异常阻止写入。
     */
    @Override
    public void beforeSave(@NonNull BaseUserPoolEntityDraft draft, @Nullable BaseUserPoolEntity original) {
        if (UserPoolContextHolder.isIgnore()) {
            return;
        }
        UUID userPoolId = UserPoolContextHolder.getUserPoolId();
        if (userPoolId == null) {
            return;
        }
        if (original != null && !userPoolId.equals(original.userPoolId())) {
            throw mismatch();
        }
        if (ImmutableObjects.isLoaded(draft, BaseUserPoolEntityProps.USER_POOL_ID)) {
            if (!userPoolId.equals(draft.userPoolId())) {
                throw mismatch();
            }
        } else {
            draft.setUserPoolId(userPoolId);
        }
    }

    /** 声明本拦截器关心的属性（userPoolId），jimmer 据此决定何时触发回调。 */
    @Override
    public Collection<TypedProp<BaseUserPoolEntity, ?>> dependencies() {
        return List.of(BaseUserPoolEntityProps.USER_POOL_ID);
    }

    /** 构造“数据用户池与当前用户池不一致”的领域异常。 */
    private static DomainException mismatch() {
        return new DomainException(ErrorCodeConstants.USER_POOL_CONTEXT_MISMATCH);
    }
}
