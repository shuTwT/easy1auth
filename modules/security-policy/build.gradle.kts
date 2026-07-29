plugins { id("easy1auth.java-library") }
dependencies {
    api(project(":modules:foundation"))
    implementation(project(":modules:persistence"))
    implementation(libs.spring.security)
    annotationProcessor(libs.jimmer.apt)
    testImplementation(libs.spring.test)
}
