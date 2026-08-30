plugins { id("easy1auth.java-library") }
dependencies {
    api(project(":common"))
    implementation(libs.spring.security)
    annotationProcessor(libs.jimmer.apt)
    testImplementation(libs.spring.test)
}
