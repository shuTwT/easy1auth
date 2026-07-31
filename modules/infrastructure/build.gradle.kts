plugins { id("easy1auth.java-library") }

dependencies {
    api(libs.spring.web)
    api(libs.jimmer.spring)
    annotationProcessor(libs.jimmer.apt)
    runtimeOnly(libs.postgresql)
    testImplementation(libs.spring.test)
}
