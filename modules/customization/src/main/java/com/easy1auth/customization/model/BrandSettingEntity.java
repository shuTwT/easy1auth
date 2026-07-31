package com.easy1auth.customization.model;

import com.easy1auth.persistence.model.BaseEntity;
import com.easy1auth.persistence.model.BaseTenantEntity;
import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "brand_setting")
public interface BrandSettingEntity extends BaseEntity, BaseTenantEntity {

    @Serialized
    Map<String, Object> settings();

    @Nullable
    @Column(name = "company_name")
    String companyName();

    @Nullable String logo();

    @Nullable String favicon();

    @Column(name = "primary_color")
    String primaryColor();

    @Nullable
    @Column(name = "secondary_color")
    String secondaryColor();

    @Nullable
    @Column(name = "support_email")
    String supportEmail();

    @Nullable
    @Column(name = "copyright_text")
    String copyrightText();

    @Column(name = "updated_at")
    Instant updatedAt();
}
