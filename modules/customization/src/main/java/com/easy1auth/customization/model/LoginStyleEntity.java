package com.easy1auth.customization.model;

import com.easy1auth.common.persistence.model.BaseEntity;
import com.easy1auth.common.persistence.model.BaseTenantEntity;
import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.*;

/**
 * 登录样式实体（对应 login_style 表）。
 *
 * <p>描述 pool_user 登录页的品牌与样式配置，包括 logo、配色、文案、
 * 背景、自定义 CSS、登录方式与社交身份源等。采用"草稿 + 发布"模式：
 * 编辑保存在草稿配置中，发布后对登录页生效；同时保留旧版字段
 * （logo、title 等）用于向前兼容与配置降级回退。</p>
 */
@Entity
@Table(name = "login_style")
public interface LoginStyleEntity extends BaseEntity, BaseTenantEntity {

    /** 亮色主题 logo 图片 URL（可为空） */
    @Nullable String logo();

    /** 深色主题 logo 图片 URL（可为空） */
    @Nullable
    @Column(name = "logo_dark")
    String logoDark();

    /** 背景图片 URL（可为空，为空时使用纯色背景） */
    @Nullable
    @Column(name = "background_image")
    String backgroundImage();

    /** 背景颜色（六位十六进制，如 #f5f7fa） */
    @Column(name = "background_color")
    String backgroundColor();

    /** 主题主色（六位十六进制，如 #0369A1） */
    @Column(name = "primary_color")
    String primaryColor();

    /** 登录页标题文案 */
    String title();

    /** 登录页副标题文案 */
    String subtitle();

    /** 自定义 CSS（不允许含脚本、@import 等不安全内容） */
    @Nullable
    @Column(name = "custom_css")
    String customCss();

    /** 已发布的登录方式列表：password / email / social */
    @Serialized
    @Column(name = "login_methods")
    List<String> loginMethods();

    /** 已发布的社交身份源 ID 列表 */
    @Serialized
    @Column(name = "social_providers")
    List<String> socialProviders();

    /** 草稿中的社交身份源 ID 列表（发布前编辑中） */
    @Serialized
    @Column(name = "draft_social_providers")
    List<String> draftSocialProviders();

    /** 草稿中的完整样式配置（结构化 JSON，含 schemaVersion） */
    @Serialized
    @Column(name = "draft_config")
    Map<String, Object> draftConfig();

    /** 已发布的完整样式配置（结构化 JSON，含 schemaVersion） */
    @Serialized
    @Column(name = "published_config")
    Map<String, Object> publishedConfig();

    /** 是否开放注册：true 表示登录页允许新用户注册 */
    @Column(name = "registration_enabled")
    boolean registrationEnabled();

    /** 草稿最后更新时间 */
    @Column(name = "draft_updated_at")
    Instant draftUpdatedAt();

    /** 最近发布时间，从未发布为 null */
    @Nullable
    @Column(name = "published_at")
    Instant publishedAt();

    /** 创建时间 */
    @Column(name = "created_at")
    Instant createdAt();

    /** 最后更新时间 */
    @Column(name = "updated_at")
    Instant updatedAt();
}
