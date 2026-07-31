package com.easy1auth.persistence.model;

import org.babyfish.jimmer.sql.Id;
import org.babyfish.jimmer.sql.MappedSuperclass;

import java.util.UUID;

@MappedSuperclass
public interface BaseEntity {
    @Id
    UUID id();
}
