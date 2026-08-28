package com.easy1auth.customization.model;

import com.easy1auth.infrastructure.persistence.model.BaseEntity;
import com.easy1auth.infrastructure.persistence.model.BaseTenantEntity;
import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Table;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * 法律文档设置实体（对应 legal_document_setting 表）。
 *
 * <p>保存登录 / 注册页展示的服务条款与隐私政策。采用"草稿 + 已发布"
 * 双版本结构：编辑保存到草稿，只有发布后的版本对用户可见。</p>
 */
@Entity
@Table(name = "legal_document_setting")
public interface LegalDocumentSettingEntity extends BaseEntity, BaseTenantEntity {

    /** 服务条款（历史生效值，未发布草稿时的兜底内容） */
    @Nullable
    @Column(name = "terms_of_service")
    String termsOfService();

    /** 隐私政策（历史生效值，未发布草稿时的兜底内容） */
    @Nullable
    @Column(name = "privacy_policy")
    String privacyPolicy();

    /** 服务条款草稿（编辑中、尚未发布） */
    @Nullable
    @Column(name = "draft_terms_of_service")
    String draftTermsOfService();

    /** 隐私政策草稿（编辑中、尚未发布） */
    @Nullable
    @Column(name = "draft_privacy_policy")
    String draftPrivacyPolicy();

    /** 已发布的服务条款，对用户可见 */
    @Nullable
    @Column(name = "published_terms_of_service")
    String publishedTermsOfService();

    /** 已发布的隐私政策，对用户可见 */
    @Nullable
    @Column(name = "published_privacy_policy")
    String publishedPrivacyPolicy();

    /** 最后更新时间 */
    @Column(name = "updated_at")
    Instant updatedAt();
}
