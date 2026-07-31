package com.easy1auth.security.model;

import org.babyfish.jimmer.sql.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "password_history")
public interface PasswordHistoryEntity {
    @Id
    UUID id();

    @Column(name = "subject_type")
    String subjectType();

    @Column(name = "subject_id")
    UUID subjectId();

    @Column(name = "password_hash")
    String passwordHash();

    @Column(name = "created_at")
    Instant createdAt();
}
