package com.easy1auth.customization.repository;

import com.easy1auth.customization.model.*;
import org.babyfish.jimmer.sql.JSqlClient;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** 品牌定制相关表的数据访问仓储。 */
@Repository
public class CustomizationRepository {
    private static final LegalDocumentSettingEntityTable LEGAL = LegalDocumentSettingEntityTable.$;
    private static final LoginStyleEntityTable STYLE = LoginStyleEntityTable.$;
    private static final CustomDomainEntityTable DOMAIN = CustomDomainEntityTable.$;
    private static final MessageTemplateEntityTable TEMPLATE = MessageTemplateEntityTable.$;

    private final JSqlClient sql;

    public CustomizationRepository(JSqlClient sql) { this.sql = sql; }

    public Optional<LegalDocumentSettingEntity> legalDocuments() {
        return sql.createQuery(LEGAL).select(LEGAL).fetchOptional();
    }
    public Optional<LegalDocumentSettingEntity> legalDocuments(UUID tenant) {
        return sql.createQuery(LEGAL).where(LEGAL.tenantId().eq(tenant)).select(LEGAL).fetchOptional();
    }
    public void insertLegal(LegalDocumentSettingEntity value) { sql.saveCommand(value).setMode(SaveMode.INSERT_IF_ABSENT).execute(); }
    public void upsertLegal(LegalDocumentSettingEntity value) { sql.saveCommand(value).setMode(SaveMode.UPSERT).execute(); }

    public Optional<LoginStyleEntity> style() { return sql.createQuery(STYLE).select(STYLE).fetchOptional(); }
    public Optional<LoginStyleEntity> style(UUID tenant) { return sql.createQuery(STYLE).where(STYLE.tenantId().eq(tenant)).select(STYLE).fetchOptional(); }
    public void insertStyle(LoginStyleEntity value) { sql.saveCommand(value).setMode(SaveMode.INSERT_IF_ABSENT).execute(); }
    public void upsertStyle(LoginStyleEntity value) { sql.saveCommand(value).setMode(SaveMode.UPSERT).execute(); }

    public List<CustomDomainEntity> domains() { return sql.createQuery(DOMAIN).orderBy(DOMAIN.createdAt().desc()).select(DOMAIN).execute(); }
    public void insertDomain(CustomDomainEntity value) { sql.saveCommand(value).setMode(SaveMode.INSERT_ONLY).execute(); }
    public boolean deleteDomain(UUID id) {
        if (!sql.createQuery(DOMAIN).where(DOMAIN.id().eq(id)).select(DOMAIN.id()).exists()) return false;
        return sql.deleteById(CustomDomainEntity.class, id).getTotalAffectedRowCount() == 1;
    }

    public List<MessageTemplateEntity> templates(String type) {
        return sql.createQuery(TEMPLATE).whereIf(type != null && !type.isBlank(), () -> TEMPLATE.type().eq(type))
                .orderBy(TEMPLATE.type(), TEMPLATE.code()).select(TEMPLATE).execute();
    }
    public void insertTemplate(MessageTemplateEntity value) { sql.saveCommand(value).setMode(SaveMode.INSERT_ONLY).execute(); }
    public Optional<MessageTemplateEntity> template(UUID id) { return sql.createQuery(TEMPLATE).where(TEMPLATE.id().eq(id)).select(TEMPLATE).fetchOptional(); }
    public int updateTemplate(UUID id, String type, String code, String name, String subject, String content,
                              Map<String, String> variables, boolean defaultTemplate, String status) {
        return sql.createUpdate(TEMPLATE).set(TEMPLATE.type(), type).set(TEMPLATE.code(), code).set(TEMPLATE.name(), name)
                .set(TEMPLATE.subject(), subject).set(TEMPLATE.content(), content).set(TEMPLATE.variables(), variables)
                .set(TEMPLATE.defaultTemplate(), defaultTemplate).set(TEMPLATE.status(), status)
                .set(TEMPLATE.updatedAt(), java.time.Instant.now()).where(TEMPLATE.id().eq(id)).execute();
    }
    public boolean deleteTemplate(UUID id) { return sql.deleteById(MessageTemplateEntity.class, id).getTotalAffectedRowCount() == 1; }
}
