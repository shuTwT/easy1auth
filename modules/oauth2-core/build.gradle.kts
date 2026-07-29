plugins { id("easy1auth.java-library") }
dependencies {
    api(project(":modules:foundation"))
    implementation(project(":modules:application"))
    implementation(project(":modules:tenant"))
    implementation(project(":modules:persistence"))
    implementation(libs.spring.oauth.authorization.server)
    implementation(libs.spring.jdbc)
    annotationProcessor(libs.jimmer.apt)
    testImplementation(libs.spring.test)
}
