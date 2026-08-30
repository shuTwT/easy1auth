plugins { id("easy1auth.java-library") }
dependencies {
 api(project(":common"))
 implementation(project(":modules:tenant"))
 implementation(project(":modules:security-policy"))
 annotationProcessor(libs.jimmer.apt)
}
