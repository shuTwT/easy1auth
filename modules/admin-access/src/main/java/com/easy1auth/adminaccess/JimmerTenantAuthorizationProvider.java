package com.easy1auth.adminaccess;
import com.easy1auth.adminaccess.model.*; import com.easy1auth.adminidentity.model.*; import com.easy1auth.tenant.TenantAuthorizationProvider; import org.babyfish.jimmer.sql.JSqlClient; import org.springframework.stereotype.Component; import java.util.*; import java.util.stream.Collectors;
@Component final class JimmerTenantAuthorizationProvider implements TenantAuthorizationProvider{
 private static final AdminAccountEntityTable ACCOUNT=AdminAccountEntityTable.$; private static final AdminRoleEntityTable ROLE=AdminRoleEntityTable.$; private final JSqlClient sql;
 JimmerTenantAuthorizationProvider(JSqlClient sql){this.sql=sql;}
 public boolean isActiveAccount(UUID id){return sql.createQuery(ACCOUNT).where(ACCOUNT.id().eq(id),ACCOUNT.status().eq("active")).select(ACCOUNT.id()).exists();}
 public Set<String> roles(UUID membership,String membershipRole){var result=sql.createQuery(ROLE).where(ROLE.memberships(m->m.id().eq(membership))).select(ROLE.name()).execute().stream().collect(Collectors.toSet());result.add(membershipRole);return result;}
 public Set<String> permissions(UUID membership,String membershipRole){if("owner".equals(membershipRole))return Set.of("*");return sql.createQuery(ROLE).where(ROLE.memberships(m->m.id().eq(membership))).select(ROLE.permissions()).execute().stream().flatMap(Collection::stream).collect(Collectors.toUnmodifiableSet());}
}
