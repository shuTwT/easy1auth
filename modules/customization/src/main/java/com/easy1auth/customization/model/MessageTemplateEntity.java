package com.easy1auth.customization.model;

import org.babyfish.jimmer.sql.*;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "message_template")
public interface MessageTemplateEntity {
    @Id
    UUID id();

    @Column(name = "tenant_id")
    UUID tenantId();

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
