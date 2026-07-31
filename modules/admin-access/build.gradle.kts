plugins { id("easy1auth.java-library") }
dependencies {
    api(project(":modules:infrastructure")); implementation(project(":modules:tenant")); implementation(project(":modules:admin-identity"))
    annotationProcessor(libs.jimmer.apt)
    testImplementation(libs.spring.test)
}
