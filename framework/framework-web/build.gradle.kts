plugins {
    id("easy1auth.java-library")
}

dependencies {
    api(project(":framework:framework-common"))
    api(libs.spring.web)
    testImplementation(libs.spring.test)
}
