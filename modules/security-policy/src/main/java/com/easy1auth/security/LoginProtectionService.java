package com.easy1auth.security;
import com.easy1auth.foundation.error.DomainException; import com.easy1auth.foundation.id.UuidV7; import com.easy1auth.security.model.*;
import org.babyfish.jimmer.sql.JSqlClient; import org.babyfish.jimmer.sql.ast.Predicate; import org.babyfish.jimmer.sql.ast.mutation.SaveMode; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional;
import java.time.*; import java.util.*;
@Service public class LoginProtectionService {
 private static final LoginFailureStateEntityTable STATE=LoginFailureStateEntityTable.$; private final JSqlClient sql;
 public LoginProtectionService(JSqlClient sql){this.sql=sql;}
 @Transactional(readOnly=true) public void assertAllowed(String type,String key,UUID tenant){var row=find(type,normalize(key),tenant,false);if(row!=null&&row.lockedUntil()!=null&&row.lockedUntil().isAfter(Instant.now()))throw new DomainException("ACCOUNT_TEMPORARILY_LOCKED","登录失败次数过多，请稍后再试",423);}
 @Transactional public void failed(String type,String key,UUID tenant,int limit,int lockSeconds){String normalized=normalize(key);var row=find(type,normalized,tenant,true);Instant now=Instant.now();if(row==null){var e=LoginFailureStateEntityDraft.$.produce(d->d.setId(UuidV7.randomUuid()).setSubjectType(type).setSubjectKey(normalized).setTenantId(tenant).setFailedAttempts(1).setLockedUntil(limit<=1?now.plusSeconds(lockSeconds):null).setLastFailedAt(now));sql.saveCommand(e).setMode(SaveMode.INSERT_IF_ABSENT).execute();return;}int failures=(row.lockedUntil()!=null&&row.lockedUntil().isBefore(now)?0:row.failedAttempts())+1;sql.createUpdate(STATE).set(STATE.failedAttempts(),failures).set(STATE.lastFailedAt(),now).set(STATE.lockedUntil(),failures>=limit?now.plusSeconds(lockSeconds):null).where(STATE.id().eq(row.id())).execute();}
 @Transactional public void succeeded(String type,String key,UUID tenant){sql.createDelete(STATE).where(STATE.subjectType().eq(type),STATE.subjectKey().eq(normalize(key)),tenant==null?STATE.tenantId().isNull():STATE.tenantId().eq(tenant)).execute();}
 private LoginFailureStateEntity find(String type,String key,UUID tenant,boolean lock){var q=sql.createQuery(STATE).where(STATE.subjectType().eq(type),STATE.subjectKey().eq(key),tenant==null?STATE.tenantId().isNull():STATE.tenantId().eq(tenant)).select(STATE);return lock?q.forUpdate().fetchOneOrNull():q.fetchOneOrNull();}
 private static String normalize(String key){return Objects.toString(key,"").strip().toLowerCase(Locale.ROOT).substring(0,Math.min(400,Objects.toString(key,"").strip().length()));}
}
