plugins { id("easy1auth.java-library") }

dependencies {
    api(project(":modules:foundation"))
    implementation(project(":modules:persistence"))
    annotationProcessor(libs.jimmer.apt)
    testImplementation(libs.spring.test)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgres)
}
