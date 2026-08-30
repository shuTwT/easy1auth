plugins { id("easy1auth.java-library") }

dependencies {
    api(project(":common"))
    implementation(project(":modules:tenant"))
    implementation(project(":modules:pool-identity"))
    implementation(project(":modules:security-policy"))
    implementation(libs.spring.web)
    annotationProcessor(libs.jimmer.apt)
}
