plugins { id("easy1auth.java-library") }
dependencies {
    api(project(":modules:infrastructure"))
    implementation(project(":modules:tenant"))
    implementation(libs.spring.security)
    annotationProcessor(libs.jimmer.apt)
    testImplementation(libs.spring.test)
}
