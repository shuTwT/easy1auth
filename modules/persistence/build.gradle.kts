plugins { id("easy1auth.java-library") }

dependencies {
    api(project(":modules:foundation"))
    api(libs.jimmer.spring)
    annotationProcessor(libs.jimmer.apt)
    runtimeOnly(libs.postgresql)
    testImplementation(libs.spring.test)
}
