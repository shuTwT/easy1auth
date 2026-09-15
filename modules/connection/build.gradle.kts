plugins { id("easy1auth.java-library") }
dependencies {
    api(project(":framework:framework-common"))
    api(project(":framework:framework-persistence"))
    api(project(":framework:framework-tenant"))
    implementation(project(":modules:security-policy"))
    implementation(project(":modules:pool-identity"))
    implementation(project(":modules:tenant"))
    implementation(libs.spring.web)
    implementation(libs.spring.oauth.resource.server)
 annotationProcessor(libs.jimmer.apt)
}
