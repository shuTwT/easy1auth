package com.easy1auth.common.tenant.aop;

import com.easy1auth.common.tenant.annotation.TenantIgnore;
import com.easy1auth.common.foundation.util.TenantContextHolder;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

@Aspect
public class TenantIgnoreAspect {

    @Around("@annotation(tenantIgnore)")
    public Object around(ProceedingJoinPoint joinPoint, TenantIgnore tenantIgnore) throws Throwable{
        boolean oldIgnore = TenantContextHolder.isIgnore();
        try {
            TenantContextHolder.setIgnore(true);
            return joinPoint.proceed();
        }finally {
            TenantContextHolder.setIgnore(oldIgnore);
        }
    }
}
