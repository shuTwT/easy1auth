package com.easy1auth.admin;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureTest {
    @Test void tenantRepositoryIsNotAccessedAcrossModuleBoundary() {
        var classes = new ClassFileImporter().importPackages("com.easy1auth");
        noClasses().that().resideOutsideOfPackage("com.easy1auth.tenant..")
                .should().accessClassesThat().haveSimpleName("TenantRepository")
                .check(classes);
    }
    @Test void repositoriesMustNotDependOnSpringJdbcClient() {
        var classes = new ClassFileImporter().importPackages("com.easy1auth");
        noClasses().that().haveSimpleNameEndingWith("Repository")
                .should().dependOnClassesThat().haveFullyQualifiedName("org.springframework.jdbc.core.simple.JdbcClient")
                .check(classes);
    }
}
