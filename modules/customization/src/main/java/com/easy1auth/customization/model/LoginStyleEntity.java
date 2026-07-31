package com.easy1auth.customization.model;

import com.easy1auth.persistence.model.BaseEntity;
import com.easy1auth.persistence.model.BaseTenantEntity;
import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "login_style")
public interface LoginStyleEntity extends BaseEntity, BaseTenantEntity {

    @Nullable String logo();

    @Nullable
    @Column(name = "logo_dark")
    String logoDark();

    @Nullable
    @Column(name = "background_image")
    String backgroundImage();

    @Column(name = "background_color")
    String backgroundColor();

    @Column(name = "primary_color")
    String primaryColor();

    String title();

    String subtitle();

    @Nullable
    @Column(name = "custom_css")
    String customCss();

    @Serialized
    @Column(name = "login_methods")
    List<String> loginMethods();

    @Serialized
    @Column(name = "social_providers")
    List<String> socialProviders();

    @Column(name = "created_at")
    Instant createdAt();

    @Column(name = "updated_at")
    Instant updatedAt();
}
