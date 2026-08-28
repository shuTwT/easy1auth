package com.easy1auth.audit.model;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.*;

/**
 * 投递队列实体（对应 delivery_outbox 表）。
 *
 * <p>outbox 模式的核心表：所有审计事件/邮件的投递任务先落此表，再由调度器
 * 领取投递，保证审计事件与外部投递之间的事务一致性。状态机：
 * pending → processing → sent / dead；processing 带 lease_until 租约，
 * 租约超时后可由其他实例重新领取，避免重复投递。</p>
 */
@Entity
@Table(name = "delivery_outbox")
public interface DeliveryOutboxEntity {
    /** 投递记录 ID */
    @Id
    UUID id();

    /** 所属租户 ID（可为 null） */
    @Nullable
    @Column(name = "tenant_id")
    UUID tenantId();

    /** 投递渠道：webhook / email */
    String channel();

    /** 投递目标：webhook 为回调地址，email 为收件地址 */
    String destination();

    /** 事件类型（如 "email"、审计事件名） */
    @Column(name = "event_type")
    String eventType();

    /** 投递负载（序列化存储，webhook 为事件内容，email 含 subject/body） */
    @Serialized
    Map<String, Object> payload();

    /** 关联的 Webhook 订阅 ID（邮件投递为 null） */
    @Nullable
    @Column(name = "subscription_id")
    UUID subscriptionId();

    /** 幂等键（防止同一事件被重复投递） */
    @Column(name = "idempotency_key")
    String idempotencyKey();

    /** 投递状态：pending / processing / sent / dead */
    String status();

    /** 已尝试投递次数 */
    int attempts();

    /** 最大投递尝试次数 */
    @Column(name = "max_attempts")
    int maxAttempts();

    /** 最早可投递时间（退避期间会延后） */
    @Column(name = "available_at")
    Instant availableAt();

    /** processing 状态的租约截止时间（超时后可被重新领取，可为 null） */
    @Nullable
    @Column(name = "lease_until")
    Instant leaseUntil();

    /** 最近一次失败原因（可为 null） */
    @Nullable
    @Column(name = "last_error")
    String lastError();

    /** 创建时间 */
    @Column(name = "created_at")
    Instant createdAt();

    /** 投递成功时间（可为 null） */
    @Nullable
    @Column(name = "sent_at")
    Instant sentAt();
}
