package com.easy1auth.admin.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

@Component
public final class SchemaValidation implements ApplicationRunner {
    private final JdbcClient db;

    SchemaValidation(JdbcClient db) {
        this.db = db;
    }

    @Override
    public void run(ApplicationArguments args) {
        Boolean applied = db.sql("select exists(select 1 from flyway_schema_history where version='1' and success=true)")
                .query(Boolean.class).single();
        if (!Boolean.TRUE.equals(applied)) {
            throw new IllegalStateException("Database schema V1 is not initialized; run the database-migration image first");
        }
    }
}
