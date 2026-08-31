plugins { id("easy1auth.spring-application") }

dependencies {
    implementation(project(":common"))
    implementation(project(":modules:tenant"))
    implementation(project(":modules:admin-identity"))
    implementation(project(":modules:admin-access"))
    implementation(project(":modules:pool-identity"))
    implementation(project(":modules:application"))
    implementation(project(":modules:security-policy"))
    implementation(project(":modules:connection"))
    implementation(project(":modules:customization"))
    implementation(project(":modules:audit"))
    implementation(libs.spring.web)
    implementation(libs.spring.actuator)
    implementation(libs.spring.security)
    implementation(libs.spring.oauth.resource.server)
    implementation(libs.spring.jdbc)
    implementation(libs.spring.mail)
    implementation(libs.poi.ooxml)
    runtimeOnly(libs.postgresql)
    testImplementation(libs.spring.test)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgres)
    testImplementation(libs.archunit)
    testImplementation(libs.flyway.core)
    testImplementation(libs.flyway.postgres)
}

sourceSets { test { resources.srcDir("../../database-migration/src/main/resources") } }
