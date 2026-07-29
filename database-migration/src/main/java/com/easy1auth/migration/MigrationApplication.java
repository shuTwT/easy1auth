package com.easy1auth.migration;

import com.easy1auth.foundation.id.UuidV7;
import com.easy1auth.foundation.security.SecretPolicy;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Types;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

@SpringBootApplication
public class MigrationApplication {
    private static final Logger LOG = LoggerFactory.getLogger(MigrationApplication.class);

    public static void main(String[] args) {
        var context = SpringApplication.run(MigrationApplication.class, args);
        System.exit(SpringApplication.exit(context));
    }

    @Bean
    ApplicationRunner migrateAndOptionallyBootstrap(Flyway flyway, JdbcClient db,
                                                     TransactionTemplate transactions,
                                                     Environment environment) {
        return args -> {
            var commands = args.getNonOptionArgs();
            Set<String> supported = Set.of("bootstrap-admin", "flyway-info", "flyway-validate");
            if (commands.size() > 1 || (!commands.isEmpty() && !supported.contains(commands.getFirst()))) {
                throw new IllegalArgumentException("Supported commands: bootstrap-admin, flyway-info, flyway-validate");
            }
            if (Arrays.asList(environment.getActiveProfiles()).contains("production")) {
                SecretPolicy.require("DATABASE_PASSWORD", environment.getProperty("spring.datasource.password"), 16);
            }

            if (!commands.isEmpty() && "flyway-info".equals(commands.getFirst())) {
                var info = flyway.info();
                long applied = Arrays.stream(info.applied()).count();
                long pending = Arrays.stream(info.pending()).count();
                LOG.info("Flyway info; currentVersion={}, applied={}, pending={}",
                        info.current() == null ? "none" : info.current().getVersion(), applied, pending);
                return;
            }
            if (!commands.isEmpty() && "flyway-validate".equals(commands.getFirst())) {
                var validation = flyway.validateWithResult();
                if (!validation.validationSuccessful) {
                    throw new IllegalStateException("Flyway validation failed: " + validation.errorDetails.errorMessage);
                }
                requireV7(flyway);
                LOG.info("Flyway validation complete; schema includes V7");
                return;
            }

            var result = flyway.migrate();
            requireV7(flyway);
            LOG.info("Flyway migration complete; schema version={}, migrations executed={}",
                    flyway.info().current().getVersion(), result.migrationsExecuted);

            if (commands.isEmpty()) return;
            if (!Arrays.asList(environment.getActiveProfiles()).contains("bootstrap-admin")) {
                throw new IllegalStateException("bootstrap-admin command requires the bootstrap-admin Spring profile");
            }
            bootstrapAdmin(db, transactions, environment);
        };
    }

    private static void requireV7(Flyway flyway) {
        boolean applied = Arrays.stream(flyway.info().applied())
                .anyMatch(info -> info.getVersion() != null && "7".equals(info.getVersion().getVersion()));
        if (!applied) throw new IllegalStateException("Flyway migration V7 must be applied before startup");
    }

    private static void bootstrapAdmin(JdbcClient db, TransactionTemplate transactions, Environment environment) {
        String username = required(environment, "easy1auth.bootstrap.username").strip();
        String email = required(environment, "easy1auth.bootstrap.email").strip().toLowerCase();
        String password = SecretPolicy.require("BOOTSTRAP_ADMIN_PASSWORD",
                required(environment, "easy1auth.bootstrap.password"), 8);
        validateIdentity(username, email, password);

        var encoder = new BCryptPasswordEncoder(12);
        String passwordHash = encoder.encode(password);
        if (!passwordHash.matches("^\\$2[aby]\\$12\\$.*") || !encoder.matches(password, passwordHash)) {
            throw new IllegalStateException("Could not create a BCrypt cost-12 password hash");
        }

        UUID accountId = UuidV7.randomUuid();
        transactions.executeWithoutResult(status -> {
            db.sql("select pg_advisory_xact_lock(1163283534) as bootstrap_lock").query().singleRow();
            Integer count = db.sql("select count(*) from admin_account").query(Integer.class).single();
            if (count != null && count > 0) {
                throw new IllegalStateException("bootstrap-admin refused: an administrator already exists");
            }
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            db.sql("insert into admin_account (id, username, email, status, security_version, created_at, updated_at) " +
                            "values (:id, :username, :email, 'active', 1, :now, :now)")
                    .param("id", accountId).param("username", username).param("email", email)
                    .param("now", now, Types.TIMESTAMP_WITH_TIMEZONE)
                    .update();
            db.sql("insert into admin_credential (account_id, password_hash, password_changed_at, created_at, updated_at) " +
                            "values (:id, :hash, :now, :now, :now)")
                    .param("id", accountId).param("hash", passwordHash)
                    .param("now", now, Types.TIMESTAMP_WITH_TIMEZONE).update();
        });
        LOG.info("Initial administrator created; accountId={}", accountId);
    }

    private static String required(Environment environment, String property) {
        String value = environment.getProperty(property);
        if (value == null || value.isBlank()) throw new IllegalStateException(property + " is required");
        return value;
    }

    private static void validateIdentity(String username, String email, String password) {
        if (username.isBlank() || username.length() > 100) {
            throw new IllegalStateException("Bootstrap administrator username must contain 1-100 characters");
        }
        if (email.length() > 320 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalStateException("Bootstrap administrator email is invalid");
        }
        if (password.length() > 128 || !password.matches(".*[a-z].*")
                || !password.matches(".*[A-Z].*") || !password.matches(".*\\d.*")) {
            throw new IllegalStateException("Bootstrap administrator password must be 8-128 characters and include upper-case, lower-case, and numeric characters");
        }
        String normalizedPassword = password.toLowerCase();
        if (normalizedPassword.equals(username.toLowerCase()) || normalizedPassword.equals(email.toLowerCase())) {
            throw new IllegalStateException("Bootstrap administrator password must not equal the username or email");
        }
    }
}
