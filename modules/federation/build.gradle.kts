plugins { id("easy1auth.java-library") }
dependencies {
 api(project(":modules:foundation")); implementation(project(":modules:persistence"))
 implementation(project(":modules:security-policy")); implementation(project(":modules:directory"))
 implementation(project(":modules:tenant"))
 implementation(libs.spring.web); implementation(libs.spring.oauth.resource.server)
 annotationProcessor(libs.jimmer.apt)
}
