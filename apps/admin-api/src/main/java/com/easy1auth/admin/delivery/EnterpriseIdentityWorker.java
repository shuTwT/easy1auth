package com.easy1auth.admin.delivery;

import com.easy1auth.enterpriseidentity.service.EnterpriseIdentityService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 企业身份源轮询 worker（后台定时任务）。
 *
 * <p>周期性从企业身份源待处理队列认领任务并逐一处理，用于支撑飞书等第三方身份源
 * 的异步同步/回调处理。</p>
 */
@Component
final class EnterpriseIdentityWorker {
    /** 企业身份源服务，负责认领与处理任务 */
    private final EnterpriseIdentityService service;
    EnterpriseIdentityWorker(EnterpriseIdentityService service) { this.service = service; }
    /** 按固定间隔（默认 1 秒）认领最多 10 个任务并依次处理。 */
    @Scheduled(fixedDelayString = "${easy1auth.enterprise-identity.poll-delay:1000}")
    void poll() { service.claim(10).forEach(task -> service.process(task.id())); }
}
