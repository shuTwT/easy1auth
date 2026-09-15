plugins {
    id("easy1auth.java-library")
}

dependencies {
    api(project(":framework:framework-common"))
    api(libs.jimmer.spring)
    api(libs.jackson.annotations)
    annotationProcessor(libs.jimmer.apt)
}
