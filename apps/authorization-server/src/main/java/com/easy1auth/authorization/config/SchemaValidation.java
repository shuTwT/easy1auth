package com.easy1auth.authorization.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public final class SchemaValidation implements ApplicationRunner {
    private final JdbcClient db;

    SchemaValidation(JdbcClient db) { this.db = db; }

    @Override
    public void run(ApplicationArguments args) {
        Boolean applied = db.sql("select exists(select 1 from flyway_schema_history where version='7' and success=true)")
                .query(Boolean.class).single();
        if (!Boolean.TRUE.equals(applied)) {
            throw new IllegalStateException("Database schema is not migrated through V7; run the database-migration image first");
        }
    }
}
