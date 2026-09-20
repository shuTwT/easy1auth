package com.easy1auth.system.repository;

import com.easy1auth.system.model.SystemConfigEntity;
import com.easy1auth.system.model.SystemConfigEntityDraft;
import com.easy1auth.system.model.SystemConfigEntityTable;
import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;

import java.time.Instant;
import java.util.Optional;

/** 系统配置数据访问仓储。 */
public interface SystemConfigRepository extends JRepository<SystemConfigEntity, String> {
    /** sys_config 表静态描述符。 */
    SystemConfigEntityTable CONFIG = SystemConfigEntityTable.$;

    /** 按配置键读取配置值。 */
    default Optional<String> findValue(String key) {
        return sql()
                .createQuery(CONFIG)
                .where(CONFIG.key().eq(key))
                .select(CONFIG.value())
                .fetchOptional();
    }

    /** 按配置键加行级锁读取配置值。 */
    default Optional<String> lockValue(String key) {
        return sql()
                .createQuery(CONFIG)
                .where(CONFIG.key().eq(key))
                .select(CONFIG.value())
                .forUpdate()
                .fetchOptional();
    }

    /** 新增或更新配置项。 */
    default void put(String key, String value, String description) {
        Instant now = Instant.now();
        SystemConfigEntity entity = SystemConfigEntityDraft.$.produce(draft -> draft
                .setKey(key)
                .setValue(value)
                .setDescription(description)
                .setUpdatedAt(now));
        sql()
                .saveCommand(entity)
                .setMode(SaveMode.UPSERT)
                .execute();
    }

    /** 删除配置项，返回受影响行数。 */
    default int remove(String key) {
        return sql()
                .createDelete(CONFIG)
                .where(CONFIG.key().eq(key))
                .execute();
    }
}
