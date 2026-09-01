package com.easy1auth.customization.repository;

import com.easy1auth.customization.model.CustomDomainEntity;
import com.easy1auth.customization.model.CustomDomainEntityTable;
import com.easy1auth.customization.model.LegalDocumentSettingEntity;
import com.easy1auth.customization.model.LegalDocumentSettingEntityTable;
import com.easy1auth.customization.model.LoginStyleEntity;
import com.easy1auth.customization.model.LoginStyleEntityTable;
import com.easy1auth.customization.model.MessageTemplateEntity;
import com.easy1auth.customization.model.MessageTemplateEntityTable;
import org.babyfish.jimmer.spring.repository.JRepository;
import org.babyfish.jimmer.sql.ast.mutation.SaveMode;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** 品牌定制相关表的数据访问仓储。 */
public interface CustomizationRepository extends JRepository<LoginStyleEntity, UUID> {
    LegalDocumentSettingEntityTable LEGAL = LegalDocumentSettingEntityTable.$;
    LoginStyleEntityTable STYLE = LoginStyleEntityTable.$;
    CustomDomainEntityTable DOMAIN = CustomDomainEntityTable.$;
    MessageTemplateEntityTable TEMPLATE = MessageTemplateEntityTable.$;

    default Optional<LegalDocumentSettingEntity> legalDocuments() {
        return sql().createQuery(LEGAL)
                .select(LEGAL)
                .fetchOptional();
    }

    default Optional<LegalDocumentSettingEntity> legalDocuments(UUID tenant) {
        return sql().createQuery(LEGAL)
                .where(LEGAL.tenantId().eq(tenant))
                .select(LEGAL)
                .fetchOptional();
    }

    default void insertLegal(LegalDocumentSettingEntity value) {
        sql().saveCommand(value)
                .setMode(SaveMode.INSERT_IF_ABSENT)
                .execute();
    }

    default void upsertLegal(LegalDocumentSettingEntity value) {
        sql().saveCommand(value)
                .setMode(SaveMode.UPSERT)
                .execute();
    }

    default Optional<LoginStyleEntity> style() {
        return sql().createQuery(STYLE)
                .select(STYLE)
                .fetchOptional();
    }

    default Optional<LoginStyleEntity> style(UUID tenant) {
        return sql().createQuery(STYLE)
                .where(STYLE.tenantId().eq(tenant))
                .select(STYLE)
                .fetchOptional();
    }

    default void insertStyle(LoginStyleEntity value) {
        sql().saveCommand(value)
                .setMode(SaveMode.INSERT_IF_ABSENT)
                .execute();
    }

    default void upsertStyle(LoginStyleEntity value) {
        sql().saveCommand(value)
                .setMode(SaveMode.UPSERT)
                .execute();
    }

    default List<CustomDomainEntity> domains() {
        return sql().createQuery(DOMAIN)
                .orderBy(DOMAIN.createdAt().desc())
                .select(DOMAIN)
                .execute();
    }

    default void insertDomain(CustomDomainEntity value) {
        sql().saveCommand(value)
                .setMode(SaveMode.INSERT_ONLY)
                .execute();
    }

    default boolean deleteDomain(UUID id) {
        if (!sql().createQuery(DOMAIN)
                .where(DOMAIN.id().eq(id))
                .select(DOMAIN.id())
                .exists()) {
            return false;
        }
        return sql().deleteById(CustomDomainEntity.class, id)
                .getTotalAffectedRowCount() == 1;
    }

    default List<MessageTemplateEntity> templates(String type) {
        return sql().createQuery(TEMPLATE)
                .whereIf(type != null && !type.isBlank(), () -> TEMPLATE.type().eq(type))
                .orderBy(TEMPLATE.type(), TEMPLATE.code())
                .select(TEMPLATE)
                .execute();
    }

    default void insertTemplate(MessageTemplateEntity value) {
        sql().saveCommand(value)
                .setMode(SaveMode.INSERT_ONLY)
                .execute();
    }

    default Optional<MessageTemplateEntity> template(UUID id) {
        return sql().createQuery(TEMPLATE)
                .where(TEMPLATE.id().eq(id))
                .select(TEMPLATE)
                .fetchOptional();
    }

    default int updateTemplate(
            UUID id,
            String type,
            String code,
            String name,
            String subject,
            String content,
            Map<String, String> variables,
            boolean defaultTemplate,
            String status) {
        return sql().createUpdate(TEMPLATE)
                .set(TEMPLATE.type(), type)
                .set(TEMPLATE.code(), code)
                .set(TEMPLATE.name(), name)
                .set(TEMPLATE.subject(), subject)
                .set(TEMPLATE.content(), content)
                .set(TEMPLATE.variables(), variables)
                .set(TEMPLATE.defaultTemplate(), defaultTemplate)
                .set(TEMPLATE.status(), status)
                .set(TEMPLATE.updatedAt(), Instant.now())
                .where(TEMPLATE.id().eq(id))
                .execute();
    }

    default boolean deleteTemplate(UUID id) {
        return sql().deleteById(MessageTemplateEntity.class, id)
                .getTotalAffectedRowCount() == 1;
    }
}
