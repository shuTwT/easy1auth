plugins { id("easy1auth.java-library") }
dependencies {
    api(project(":common")); implementation(project(":modules:tenant")); implementation(project(":modules:admin-identity"))
    annotationProcessor(libs.jimmer.apt)
    testImplementation(libs.spring.test)
}
