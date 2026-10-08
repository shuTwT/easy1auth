package com.easy1auth.framework.tenant.annotation;

import java.lang.annotation.*;

/**
 * 忽略用户池，标记指定方法不进行用户池的自动过滤
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface UserPoolIgnore {
}
