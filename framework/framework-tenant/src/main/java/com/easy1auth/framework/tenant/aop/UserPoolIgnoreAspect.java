package com.easy1auth.framework.tenant.aop;

import com.easy1auth.framework.tenant.annotation.UserPoolIgnore;
import com.easy1auth.framework.tenant.context.UserPoolContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class UserPoolIgnoreAspect {

    @Around("@annotation(tenantIgnore)")
    public Object around(ProceedingJoinPoint joinPoint, UserPoolIgnore tenantIgnore) throws Throwable {
        boolean oldIgnore = UserPoolContextHolder.isIgnore();
        try {
            UserPoolContextHolder.setIgnore(true);
            return joinPoint.proceed();
        } finally {
            UserPoolContextHolder.setIgnore(oldIgnore);
        }
    }
}
