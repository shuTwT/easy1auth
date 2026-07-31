plugins { id("easy1auth.spring-application") }

dependencies {
    implementation(project(":modules:infrastructure"))
    implementation(project(":modules:oauth2-core"))
    implementation(project(":modules:tenant"))
    implementation(project(":modules:admin-access"))
    implementation(project(":modules:application"))
    implementation(project(":modules:pool-identity"))
    implementation(project(":modules:security-policy"))
    implementation(project(":modules:federation"))
    implementation(project(":modules:customization"))
    implementation(project(":modules:audit"))
    implementation(libs.spring.web)
    implementation(libs.spring.actuator)
    implementation(libs.spring.security)
    implementation(libs.spring.oauth.authorization.server)
    implementation(libs.spring.jdbc)
    implementation(libs.spring.mail)
    runtimeOnly(libs.postgresql)
    testImplementation(libs.spring.test)
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgres)
    testImplementation(libs.flyway.core)
    testImplementation(libs.flyway.postgres)
}

sourceSets { test { resources.srcDir("../../database-migration/src/main/resources") } }
