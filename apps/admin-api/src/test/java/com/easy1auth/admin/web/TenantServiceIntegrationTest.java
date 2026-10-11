package com.easy1auth.admin.web;

import com.easy1auth.framework.common.error.DomainException;
import com.easy1auth.framework.tenant.context.UserPoolContextHolder;
import com.easy1auth.poolidentity.model.PoolUserEntityTable;
import com.easy1auth.system.constant.ManagementPermissionCode;
import com.easy1auth.tenant.dto.TenantInput;
import com.easy1auth.tenant.model.UserPoolEntityTable;
import com.easy1auth.tenant.service.TenantService;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.dialect.PostgresDialect;
import org.babyfish.jimmer.sql.runtime.ConnectionManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.util.UUID;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class TenantServiceIntegrationTest {
    @Container
    private static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>("postgres:17-alpine");
    private static final UUID POOL_A = UUID.randomUUID();
    private static final UUID POOL_B = UUID.randomUUID();
    private static final UUID LEGACY_CLIENT = UUID.randomUUID();
    private static final UUID LEGACY_ADMIN = UUID.randomUUID();
    private static JSqlClient sql;
    private static JdbcTemplate jdbc;
    private static TransactionTemplate transactions;
    private static TenantService tenants;
    private static int oldPermissionCount;

    @BeforeAll
    static void setup() {
        var dataSource = new DriverManagerDataSource(DATABASE.getJdbcUrl(), DATABASE.getUsername(), DATABASE.getPassword());
        jdbc = new JdbcTemplate(dataSource);
        // Exercise the upgrade path with existing pools and users, not just an empty database.
        Flyway.configure().dataSource(dataSource).target("25").load().migrate();
        oldPermissionCount = jdbc.queryForObject("select count(*) from tenant_package_permission", Integer.class);
        for (UUID poolId : new UUID[]{POOL_A, POOL_B}) {
            jdbc.update("insert into tenant(id, name, package_id) select ?, '现有用户池', id from tenant_package where code='basic'", poolId);
            jdbc.update("insert into pool_user(id, tenant_id, username, name) values (?, ?, 'existing', '现有用户')", UUID.randomUUID(), poolId);
        }
        jdbc.update("insert into tenant_package(code, name, max_users, max_apps) values ('readonly-test', '只读测试套餐', 100, 10)");
        jdbc.update("insert into tenant_package_permission(package_id, permission_code) select package.id, permission.code from tenant_package package cross join management_permission permission where package.code='readonly-test' and permission.code in ('menu:user', 'user:list', 'user:read')");
        Flyway.configure().dataSource(dataSource).target("26").load().migrate();
        jdbc.update("insert into admin_account(id, username, email, last_tenant_id) values (?, 'legacy-admin', 'admin@example.test', ?)", LEGACY_ADMIN, POOL_A);
        jdbc.update("insert into oauth_application(id, tenant_id, name, client_id) values (?, ?, 'legacy-client', 'legacy-client')", LEGACY_CLIENT, POOL_A);
        jdbc.update("insert into oauth2_authorization(id, registered_client_id, principal_name, authorization_grant_type) values ('legacy-authorization', ?, 'existing', 'authorization_code')", LEGACY_CLIENT.toString());
        jdbc.update("insert into oauth2_authorization_consent(registered_client_id, principal_name, authorities) values (?, 'existing', 'SCOPE_openid')", LEGACY_CLIENT.toString());
        Flyway.configure().dataSource(dataSource).load().migrate();
        sql = JSqlClient.newBuilder()
                .setDialect(new PostgresDialect())
                .setConnectionManager(new ConnectionManager() {
                    @Override
                    public <R> R execute(Connection supplied, Function<Connection, R> block) {
                        if (supplied != null) {
                            return block.apply(supplied);
                        }
                        Connection connection = DataSourceUtils.getConnection(dataSource);
                        try {
                            return block.apply(connection);
                        } finally {
                            DataSourceUtils.releaseConnection(connection, dataSource);
                        }
                    }
                })
                .build();
        transactions = new TransactionTemplate(new JdbcTransactionManager(dataSource));
        tenants = new TenantService(sql);
    }

    @AfterEach
    void clearContext() {
        UserPoolContextHolder.clear();
    }

    @Test
    void migrationPreservesExistingPoolsUsersAndPermissionValues() {
        assertEquals(2, jdbc.queryForObject("select count(*) from tenant", Integer.class));
        assertEquals(2, jdbc.queryForObject("select count(*) from pool_user", Integer.class));
        assertTrue(jdbc.queryForObject("select count(*) from tenant_package_permission", Integer.class) >= oldPermissionCount);
        assertEquals("用户池管理", jdbc.queryForObject("select name from management_permission where code='menu:tenant'", String.class));
        assertEquals("租户管理", jdbc.queryForObject("select name from management_permission where code='menu:pool-tenant'", String.class));
        assertTrue(jdbc.queryForObject("select count(*) from tenant_package_permission where permission_code='pool-tenant:create'", Integer.class) > 0);
        assertEquals(0, jdbc.queryForObject("select count(*) from tenant_package_permission permission join tenant_package package on permission.package_id=package.id where package.code='readonly-test' and permission.permission_code in ('pool-tenant:create', 'pool-tenant:update', 'pool-tenant:delete')", Integer.class));
        assertEquals(3, jdbc.queryForObject("select count(*) from tenant_package_permission permission join tenant_package package on permission.package_id=package.id where package.code='readonly-test' and permission.permission_code in ('menu:pool-tenant', 'pool-tenant:list', 'pool-tenant:read')", Integer.class));
        jdbc.queryForList("select code from management_permission where active", String.class).forEach(code ->
                assertTrue(ManagementPermissionCode.fromValue(code).isPresent(), "Permission enum missing: " + code));
        var table = UserPoolEntityTable.$;
        assertEquals(2, sql.createQuery(table).select(table.id()).execute().size());
    }

    @Test
    void codesAreUniqueWithinPoolAndIndependentAcrossPools() {
        var first = transactions.execute(status -> tenants.create(POOL_A, new TenantInput(" 企业甲 ", " SHARED ", "描述", "active")));
        assertNotNull(first);
        assertEquals("企业甲", first.name());
        assertEquals("shared", first.code());
        assertThrows(DomainException.class, () -> transactions.execute(status ->
                tenants.create(POOL_A, new TenantInput("重复", "shared", null, "active"))));
        var second = transactions.execute(status -> tenants.create(POOL_B, new TenantInput("企业乙", "shared", null, "active")));
        assertNotNull(second);
        assertNotEquals(first.id(), second.id());
        assertEquals(POOL_B, second.userPoolId());
    }

    @Test
    void allReadsAndWritesStayWithinOwningPool() {
        var created = transactions.execute(status -> tenants.create(POOL_A, new TenantInput("隔离测试", "isolation", null, "active")));
        assertNotNull(created);
        assertTrue(tenants.list(POOL_B, 1, 20, "隔离测试", null).items().isEmpty());
        assertThrows(DomainException.class, () -> tenants.get(POOL_B, created.id()));
        assertThrows(DomainException.class, () -> transactions.execute(status -> tenants.update(POOL_B, created.id(),
                new TenantInput("越权修改", "isolation", null, "inactive"))));
        assertThrows(DomainException.class, () -> transactions.executeWithoutResult(status -> tenants.delete(POOL_B, created.id())));
        assertEquals("隔离测试", tenants.get(POOL_A, created.id()).name());
        var updated = transactions.execute(status -> tenants.update(POOL_A, created.id(), new TenantInput("更新后", "isolation", "", "inactive")));
        assertNotNull(updated);
        assertEquals("inactive", updated.status());
        transactions.executeWithoutResult(status -> tenants.delete(POOL_A, created.id()));
        assertThrows(DomainException.class, () -> tenants.get(POOL_A, created.id()));
    }

    @Test
    void tenantStateDoesNotChangeManagementContextOrPoolUsers() {
        UserPoolContextHolder.setUserPoolId(POOL_A);
        var tenant = transactions.execute(status -> tenants.create(POOL_A, new TenantInput("未启用租户", "metadata-only", null, "inactive")));
        assertNotNull(tenant);
        assertEquals(POOL_A, UserPoolContextHolder.requireUserPoolId());
        assertEquals("active", jdbc.queryForObject("select status from tenant where id=?", String.class, POOL_A));
        var users = PoolUserEntityTable.$;
        assertEquals("existing", sql.createQuery(users).where(users.userPoolId().eq(POOL_A)).select(users.username()).fetchOne());
        transactions.executeWithoutResult(status -> tenants.delete(POOL_A, tenant.id()));
        assertEquals(POOL_A, UserPoolContextHolder.requireUserPoolId());
        assertEquals(1, jdbc.queryForObject("select count(*) from pool_user where user_pool_id=?", Integer.class, POOL_A));
    }

    @Test
    void invalidMetadataAndUnavailablePoolAreRejected() {
        assertThrows(DomainException.class, () -> transactions.execute(status -> tenants.create(POOL_A, new TenantInput(" ", "valid", null, "active"))));
        assertThrows(DomainException.class, () -> transactions.execute(status -> tenants.create(POOL_A, new TenantInput("名称", "含空格 code", null, "active"))));
        assertThrows(DomainException.class, () -> transactions.execute(status -> tenants.create(POOL_A, new TenantInput("名称", "valid", null, "suspended"))));
        assertThrows(DomainException.class, () -> transactions.execute(status -> tenants.create(UUID.randomUUID(), new TenantInput("名称", "valid", null, "active"))));
        assertEquals(1, tenants.list(POOL_A, -1, 1000, null, null).page());
        assertEquals(100, tenants.list(POOL_A, -1, 1000, null, null).pageSize());
    }

    @Test
    void renamedColumnsPreserveLegacyValuesAndCompositeForeignKeys() {
        assertEquals(0, jdbc.queryForObject("select count(*) from information_schema.columns where table_schema=current_schema() and column_name in ('tenant_id', 'last_tenant_id')", Integer.class));
        assertEquals(POOL_A, jdbc.queryForObject("select last_user_pool_id from admin_account where id=?", UUID.class, LEGACY_ADMIN));
        assertEquals(POOL_A, jdbc.queryForObject("select user_pool_id from oauth2_authorization where id='legacy-authorization'", UUID.class));
        assertEquals(POOL_A, jdbc.queryForObject("select user_pool_id from oauth2_authorization_consent where registered_client_id=?", UUID.class, LEGACY_CLIENT.toString()));
        UUID user = jdbc.queryForObject("select id from pool_user where user_pool_id=?", UUID.class, POOL_A);
        UUID ownGroup = UUID.randomUUID();
        UUID otherGroup = UUID.randomUUID();
        jdbc.update("insert into user_group(id, user_pool_id, name) values (?, ?, 'own-group'), (?, ?, 'other-group')", ownGroup, POOL_A, otherGroup, POOL_B);
        jdbc.update("insert into pool_user_group(user_pool_id, user_id, group_id) values (?, ?, ?)", POOL_A, user, ownGroup);
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () ->
                jdbc.update("insert into pool_user_group(user_pool_id, user_id, group_id) values (?, ?, ?)", POOL_A, user, otherGroup));
        assertEquals(1, jdbc.queryForObject("select count(*) from pool_user_group where user_pool_id=? and user_id=?", Integer.class, POOL_A, user));
    }

    @Test
    void oauthTriggersResolvePoolOnInsertAndClientChange() {
        UUID clientA = UUID.randomUUID();
        UUID clientB = UUID.randomUUID();
        jdbc.update("insert into oauth_application(id, user_pool_id, name, client_id) values (?, ?, 'trigger-client-a', ?), (?, ?, 'trigger-client-b', ?)",
                clientA, POOL_A, clientA.toString(), clientB, POOL_B, clientB.toString());
        jdbc.update("insert into oauth2_authorization(id, registered_client_id, principal_name, authorization_grant_type) values ('trigger-test', ?, 'existing', 'authorization_code')", clientA.toString());
        jdbc.update("insert into oauth2_authorization_consent(registered_client_id, principal_name, authorities) values (?, 'existing', 'SCOPE_openid')", clientA.toString());
        assertEquals(POOL_A, jdbc.queryForObject("select user_pool_id from oauth2_authorization where id='trigger-test'", UUID.class));
        assertEquals(POOL_A, jdbc.queryForObject("select user_pool_id from oauth2_authorization_consent where registered_client_id=?", UUID.class, clientA.toString()));
        jdbc.update("update oauth2_authorization set registered_client_id=? where id='trigger-test'", clientB.toString());
        jdbc.update("update oauth2_authorization_consent set registered_client_id=? where registered_client_id=?", clientB.toString(), clientA.toString());
        assertEquals(POOL_B, jdbc.queryForObject("select user_pool_id from oauth2_authorization where id='trigger-test'", UUID.class));
        assertEquals(POOL_B, jdbc.queryForObject("select user_pool_id from oauth2_authorization_consent where registered_client_id=?", UUID.class, clientB.toString()));
        assertThrows(org.springframework.dao.DataAccessException.class, () ->
                jdbc.update("insert into oauth2_authorization(id, registered_client_id, principal_name, authorization_grant_type) values ('invalid-client', 'missing', 'existing', 'authorization_code')"));
        jdbc.update("delete from oauth_application where id=?", clientB);
        assertEquals(0, jdbc.queryForObject("select count(*) from oauth2_authorization where id='trigger-test'", Integer.class));
        assertEquals(0, jdbc.queryForObject("select count(*) from oauth2_authorization_consent where registered_client_id=?", Integer.class, clientB.toString()));
    }
}
