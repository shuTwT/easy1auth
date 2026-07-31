plugins { id("easy1auth.spring-application") }

dependencies {
    implementation(project(":modules:infrastructure"))
    implementation(libs.spring.jdbc)
    implementation(libs.spring.security)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgres)
    runtimeOnly(libs.postgresql)
    testImplementation(libs.spring.test)
}
