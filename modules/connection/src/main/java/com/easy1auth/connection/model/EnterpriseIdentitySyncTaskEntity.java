package com.easy1auth.connection.model;

import com.easy1auth.common.persistence.model.BaseEntity;
import com.easy1auth.common.persistence.model.BaseTenantEntity;
import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Map;

/**
 * 企业身份源同步任务实体（对应 enterprise_identity_sync_task 表）。
 *
 * <p>记录一次飞书通讯录同步任务（全量或事件增量）。任务由 worker
 * 领取后按 pending -&gt; processing -&gt; succeeded / partial / failed 流转，
 * 同步结果摘要与错误信息记录在任务上。</p>
 */
@Entity
@Table(name = "enterprise_identity_sync_task")
public interface EnterpriseIdentitySyncTaskEntity extends BaseEntity, BaseTenantEntity {
    /** 所属企业身份源 ID */
    @Column(name = "source_id") java.util.UUID sourceId();
    /** 任务类型：full（全量同步）/ event（事件增量） */
    String type();
    /** 触发本任务的飞书事件 ID（仅事件同步有值） */
    @Nullable @Column(name = "event_id") String eventId();
    /** 任务负载（事件内容或全量同步附加参数） */
    @Serialized Map<String, Object> payload();
    /** 任务状态：pending（待处理）/ processing（处理中）/ succeeded（成功）/ partial（部分跳过）/ failed（失败） */
    String status();
    /** 同步结果摘要（如 createdUsers / updatedUsers / skippedEmailConflict 等计数） */
    @Serialized Map<String, Object> summary();
    /** 失败时的错误信息 */
    @Nullable @Column(name = "last_error") String lastError();
    /** 创建时间 */
    @Column(name = "created_at") Instant createdAt();
    /** 开始处理时间 */
    @Nullable @Column(name = "started_at") Instant startedAt();
    /** 完成时间 */
    @Nullable @Column(name = "finished_at") Instant finishedAt();
}
