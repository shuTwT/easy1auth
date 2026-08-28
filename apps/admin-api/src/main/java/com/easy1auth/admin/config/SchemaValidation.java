package com.easy1auth.admin.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * 启动时数据库结构校验。
 *
 * <p>应用启动后校验数据库是否已完成 Flyway V1 迁移；未完成时直接抛出异常终止启动，
 * 避免在缺失核心表结构的情况下运行。属于面向安全的快速失败（fail-fast）检查。</p>
 */
@Component
public final class SchemaValidation implements ApplicationRunner {
    /** JDBC 客户端，用于查询 Flyway 迁移记录 */
    private final JdbcClient db;

    SchemaValidation(JdbcClient db) {
        this.db = db;
    }

    /** 校验 Flyway V1 迁移是否已成功应用。 */
    @Override
    public void run(ApplicationArguments args) {
        Boolean applied = db.sql("select exists(select 1 from flyway_schema_history where version='1' and success=true)")
                .query(Boolean.class).single();
        if (!Boolean.TRUE.equals(applied)) {
            throw new IllegalStateException("Database schema V1 is not initialized; run the database-migration image first");
        }
    }
}
