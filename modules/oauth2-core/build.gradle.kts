plugins { id("easy1auth.java-library") }
dependencies {
    api(project(":common"))
    implementation(project(":modules:application"))
    implementation(project(":modules:tenant"))
    implementation(libs.spring.oauth.authorization.server)
    implementation(libs.spring.jdbc)
    annotationProcessor(libs.jimmer.apt)
    testImplementation(libs.spring.test)
}
