package com.easy1auth.adminidentity;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * 注册码原生 SQL 组件。
 *
 * <p>利用 PostgreSQL 的 DELETE ... RETURNING 特性实现注册码的一次性原子消费，
 * 防止并发场景下同一验证码被重复使用。</p>
 */
@Component
public final class RegistrationCodeNativeSql {
    /** JDBC 客户端（执行原生 SQL） */
    private final JdbcClient db;

    public RegistrationCodeNativeSql(JdbcClient db) {
        this.db = db;
    }

    /** 原子消费注册码：命中未过期的有效记录并删除则返回 true，否则返回 false。 */
    public boolean consume(String email, String hash) {
        return db.sql("delete from admin_registration_code where id in (select id from admin_registration_code where lower(email)=lower(:e) and code_hash=:h and expires_at>now() for update limit 1) returning id").param("e", email).param("h", hash).query(UUID.class).optional().isPresent();
    }

    /** 签发注册码：先清理该邮箱旧码及全部过期记录，再插入新码。 */
    public void issue(UUID id, String email, String hash, java.time.Instant expires) {
        db.sql("delete from admin_registration_code where lower(email)=lower(:e) or expires_at<=now()").param("e", email).update();
        db.sql("insert into admin_registration_code(id,email,code_hash,expires_at) values(:id,:e,:h,:x)").param("id", id).param("e", email).param("h", hash).param("x", Timestamp.from(expires)).update();
    }
}
