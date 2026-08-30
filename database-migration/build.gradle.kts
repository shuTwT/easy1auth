plugins { id("easy1auth.spring-application") }

dependencies {
    implementation(project(":common"))
    implementation(libs.spring.jdbc)
    implementation(libs.spring.security)
    implementation(libs.flyway.core)
    implementation(libs.flyway.postgres)
    runtimeOnly(libs.postgresql)
    testImplementation(libs.spring.test)
}
