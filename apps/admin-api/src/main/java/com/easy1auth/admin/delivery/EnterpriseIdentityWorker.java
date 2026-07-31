package com.easy1auth.admin.delivery;

import com.easy1auth.enterpriseidentity.EnterpriseIdentityService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
final class EnterpriseIdentityWorker {
    private final EnterpriseIdentityService service;
    EnterpriseIdentityWorker(EnterpriseIdentityService service) { this.service = service; }
    @Scheduled(fixedDelayString = "${easy1auth.enterprise-identity.poll-delay:1000}")
    void poll() { service.claim(10).forEach(task -> service.process(task.id())); }
}
