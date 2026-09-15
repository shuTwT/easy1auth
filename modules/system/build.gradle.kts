plugins { id("easy1auth.java-library") }
dependencies {
    api(project(":framework:framework-common"))
    api(project(":framework:framework-persistence"))
    api(project(":framework:framework-tenant"))
    implementation(project(":modules:tenant"))
    implementation(project(":modules:security-policy"))
    implementation(libs.spring.jdbc)
    implementation(libs.spring.security)
    annotationProcessor(libs.jimmer.apt)
    testImplementation(libs.spring.test)
}
