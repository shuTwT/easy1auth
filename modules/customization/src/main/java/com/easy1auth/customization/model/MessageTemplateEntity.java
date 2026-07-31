package com.easy1auth.customization.model;

import com.easy1auth.persistence.model.BaseEntity;
import com.easy1auth.persistence.model.BaseTenantEntity;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "message_template")
public interface MessageTemplateEntity extends BaseEntity, BaseTenantEntity {

    String type();

    String code();

    String name();

    @Nullable String subject();

    String content();

    @Serialized
    Map<String, String> variables();

    @Column(name = "is_default")
    boolean defaultTemplate();

    String status();

    @Column(name = "created_at")
    Instant createdAt();

    @Column(name = "updated_at")
    Instant updatedAt();
}
