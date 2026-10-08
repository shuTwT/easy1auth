package com.easy1auth.framework.tenant.persistence.filter;

import com.easy1auth.framework.tenant.persistence.model.BaseUserPoolEntity;
import com.easy1auth.framework.tenant.persistence.model.BaseUserPoolEntityProps;
import com.easy1auth.framework.tenant.context.UserPoolContextHolder;
import org.babyfish.jimmer.sql.filter.Filter;
import org.babyfish.jimmer.sql.filter.FilterArgs;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 用户池实体全局过滤器。
 *
 * <p>实现 jimmer 的 {@link Filter}：对所有继承 {@link BaseUserPoolEntity} 的实体查询
 * 自动追加“userPoolId = 当前用户池”的过滤条件，从读取侧实现数据层面的用户池隔离；
 * 忽略标志开启或当前无用户池上下文时不过滤。</p>
 */
@Component
public final class UserPoolEntityFilter implements Filter<BaseUserPoolEntityProps> {
    /** 为查询追加当前用户池的过滤条件（除非处于忽略模式或无用户池上下文）。 */
    @Override
    public void filter(FilterArgs<BaseUserPoolEntityProps> args) {
        if (UserPoolContextHolder.isIgnore()) {
            return;
        }
        UUID userPoolId = UserPoolContextHolder.getUserPoolId();
        if (userPoolId != null) {
            args.where(args.getTable().userPoolId().eq(userPoolId));
        }
    }
}
