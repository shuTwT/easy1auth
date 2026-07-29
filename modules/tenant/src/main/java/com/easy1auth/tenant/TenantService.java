package com.easy1auth.tenant;

import com.easy1auth.foundation.error.DomainException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
public class TenantService {
    private final TenantRepository repository;
    private final TenantAuthorizationProvider authorization;
    TenantService(TenantRepository repository, TenantAuthorizationProvider authorization) { this.repository = repository; this.authorization = authorization; }

    @Transactional
    public TenantSummary create(UUID accountId, String name, String plan) {
        if (name == null || name.isBlank()) throw new DomainException("TENANT_NAME_REQUIRED", "租户名称不能为空", 400);
        return repository.createOwned(accountId, name.strip(), plan == null || plan.isBlank() ? "basic" : plan);
    }
    @Transactional(readOnly = true) public List<TenantSummary> list(UUID accountId) { return repository.listForAccount(accountId); }
    @Transactional(readOnly = true)
    public TenantContext resolve(UUID accountId, UUID tenantId, String traceId) {
        var membership=repository.activeMembership(accountId,tenantId).filter(it->authorization.isActiveAccount(accountId)).orElseThrow(() -> new DomainException("TENANT_ACCESS_DENIED", "无权访问所选租户", 403));
        return new TenantContext(accountId,tenantId,membership.id(),membership.role(),authorization.roles(membership.id(),membership.role()),authorization.permissions(membership.id(),membership.role()),traceId);
    }
    @Transactional
    public void transferOwner(UUID actor, UUID tenantId, UUID target) {
        repository.lockTenant(tenantId);
        if (!"owner".equals(repository.role(actor, tenantId))) throw new DomainException("OWNER_REQUIRED", "仅租户所有者可转移所有权", 403);
        if (actor.equals(target)) throw new DomainException("OWNER_TARGET_SAME", "目标账号已是所有者", 400);
        if (repository.role(target, tenantId) == null) throw new DomainException("MEMBERSHIP_NOT_FOUND", "目标账号不是租户成员", 404);
        repository.transfer(tenantId, actor, target);
    }
    @Transactional
    public void removeMember(UUID actor, UUID tenantId, UUID target) {
        repository.lockTenant(tenantId);
        if (!"owner".equals(repository.role(actor, tenantId))) throw new DomainException("OWNER_REQUIRED", "仅租户所有者可移除成员", 403);
        if (actor.equals(target)) throw new DomainException("OWNER_CANNOT_REMOVE_SELF", "所有者不能移除自己，请先转移所有权", 409);
        if (repository.remove(tenantId, target) != 1) throw new DomainException("MEMBERSHIP_NOT_FOUND", "成员不存在或不能被直接移除", 404);
    }
    @Transactional(readOnly=true) public boolean isOwnerAnywhere(UUID accountId){return repository.isOwnerAnywhere(accountId);}
    @Transactional public int lockForUserQuota(UUID tenantId){return repository.lockAndGetMaxUsers(tenantId);}
    @Transactional public int lockForAppQuota(UUID tenantId){return repository.lockAndGetMaxApps(tenantId);}
    @Transactional public void lockForSecurityMaterial(UUID tenantId){repository.lockTenant(tenantId);}
}
