package com.easy1auth.oauth2;

import com.easy1auth.foundation.error.DomainException;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;

import java.net.URI;
import java.util.UUID;

public final class TenantIssuerContext {
    private TenantIssuerContext(){}
    public static UUID tenantId(){
        var context=AuthorizationServerContextHolder.getContext();
        if(context==null||context.getIssuer()==null)throw new DomainException("OAUTH_TENANT_REQUIRED","OAuth issuer 中缺少租户",400);
        return tenantId(context.getIssuer());
    }
    public static UUID tenantId(String issuer){
        String path=URI.create(issuer).getPath();String[] parts=path.split("/");
        if(parts.length<3||!"t".equals(parts[parts.length-2]))throw new DomainException("OAUTH_ISSUER_INVALID","OAuth issuer 格式无效",400);
        try{return UUID.fromString(parts[parts.length-1]);}catch(IllegalArgumentException ex){throw new DomainException("OAUTH_ISSUER_INVALID","OAuth issuer 租户标识无效",400);}
    }
}
