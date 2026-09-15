plugins {
    id("easy1auth.java-library")
}

dependencies {
    api(project(":framework:framework-common"))
    api(project(":framework:framework-persistence"))
    implementation(libs.spring.aop)
    annotationProcessor(libs.jimmer.apt)
}
