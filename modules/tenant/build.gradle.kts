plugins { id("easy1auth.java-library") }

dependencies {
    api(project(":common"))
    annotationProcessor(libs.jimmer.apt)
    testImplementation(libs.spring.test)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgres)
}
