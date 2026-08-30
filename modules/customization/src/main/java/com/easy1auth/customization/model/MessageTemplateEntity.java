package com.easy1auth.customization.model;

import com.easy1auth.common.persistence.model.BaseEntity;
import com.easy1auth.common.persistence.model.BaseTenantEntity;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.*;

/**
 * 消息模板实体（对应 message_template 表）。
 *
 * <p>用于邮件 / 短信等通知消息的模板配置，内容支持 {{变量名}} 占位符，
 * 变量须在 {@link #variables()} 中声明。可标记为默认模板，并支持启用 / 停用。</p>
 */
@Entity
@Table(name = "message_template")
public interface MessageTemplateEntity extends BaseEntity, BaseTenantEntity {

    /** 模板类型：email（邮件）/ sms（短信） */
    String type();

    /** 模板编码（小写字母数字下划线，2-100 位，唯一标识） */
    String code();

    /** 模板名称（展示用） */
    String name();

    /** 邮件主题（短信模板可空） */
    @Nullable String subject();

    /** 模板正文内容，支持 {{变量名}} 占位符 */
    String content();

    /** 模板变量声明（变量名 -> 默认值或说明） */
    @Serialized
    Map<String, String> variables();

    /** 是否为默认模板：true 表示在对应类型中作为默认兜底 */
    @Column(name = "is_default")
    boolean defaultTemplate();

    /** 模板状态：active（启用）/ disabled（停用） */
    String status();

    /** 创建时间 */
    @Column(name = "created_at")
    Instant createdAt();

    /** 最后更新时间 */
    @Column(name = "updated_at")
    Instant updatedAt();
}
