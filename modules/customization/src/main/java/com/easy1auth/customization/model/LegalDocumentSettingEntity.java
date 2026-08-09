package com.easy1auth.customization.model;

import com.easy1auth.persistence.model.BaseEntity;
import com.easy1auth.persistence.model.BaseTenantEntity;
import org.babyfish.jimmer.sql.Column;
import org.babyfish.jimmer.sql.Entity;
import org.babyfish.jimmer.sql.Table;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

@Entity
@Table(name = "legal_document_setting")
public interface LegalDocumentSettingEntity extends BaseEntity, BaseTenantEntity {

    @Nullable
    @Column(name = "terms_of_service")
    String termsOfService();

    @Nullable
    @Column(name = "privacy_policy")
    String privacyPolicy();

    @Nullable
    @Column(name = "draft_terms_of_service")
    String draftTermsOfService();

    @Nullable
    @Column(name = "draft_privacy_policy")
    String draftPrivacyPolicy();

    @Nullable
    @Column(name = "published_terms_of_service")
    String publishedTermsOfService();

    @Nullable
    @Column(name = "published_privacy_policy")
    String publishedPrivacyPolicy();

    @Column(name = "updated_at")
    Instant updatedAt();
}
