package com.easy1auth.audit.model;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.*;

/**
 * 审计事件实体（对应 audit_event 表）。
 *
 * <p>记录管理端操作审计的完整信息：谁（操作者）在何时对什么资源做了什么
 * 操作及其结果，并附带请求上下文（IP、UA、追踪 ID）与脱敏后的详情，用于
 * 安全审计与合规追踪。</p>
 */
@Entity
@Table(name = "audit_event")
public interface AuditEventEntity {
    /** 事件 ID */
    @Id
    UUID id();

    /** 所属租户 ID（可为 null） */
    @Nullable
    @Column(name = "tenant_id")
    UUID tenantId();

    /** 操作者类型：admin / user / system 等 */
    @Column(name = "actor_type")
    String actorType();

    /** 操作者账号 ID（可为 null） */
    @Nullable
    @Column(name = "actor_id")
    UUID actorId();

    /** 操作者名称（可为 null） */
    @Nullable
    @Column(name = "actor_name")
    String actorName();

    /** 事件类型（如 security / user） */
    @Column(name = "event_type")
    String eventType();

    /** 具体动作（如 create / update / login） */
    String action();

    /** 被操作资源类型 */
    @Column(name = "resource_type")
    String resourceType();

    /** 被操作资源 ID（可为 null） */
    @Nullable
    @Column(name = "resource_id")
    String resourceId();

    /** 链路追踪 ID（可为 null） */
    @Nullable
    @Column(name = "trace_id")
    String traceId();

    /** HTTP 方法（可为 null） */
    @Nullable String method();

    /** 来源 IP（可为 null） */
    @Nullable
    @Column(name = "ip_address")
    String ipAddress();

    /** 客户端 User-Agent（可为 null） */
    @Nullable
    @Column(name = "user_agent")
    String userAgent();

    /** 操作结果：success（成功）/ failure（失败） */
    String outcome();

    /** 失败时的错误码（可为 null） */
    @Nullable
    @Column(name = "error_code")
    String errorCode();

    /** 附加详情（序列化存储，敏感字段已脱敏） */
    @Serialized
    Map<String, Object> details();

    /** 事件发生时间 */
    @Column(name = "created_at")
    Instant createdAt();
}
