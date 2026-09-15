plugins { id("easy1auth.java-library") }
dependencies {
    api(project(":framework:framework-common"))
    api(project(":framework:framework-persistence"))
    api(project(":framework:framework-tenant"))
    implementation(project(":modules:application"))
    implementation(project(":modules:tenant"))
    implementation(libs.spring.oauth.authorization.server)
    implementation(libs.spring.jdbc)
    annotationProcessor(libs.jimmer.apt)
    testImplementation(libs.spring.test)
}
