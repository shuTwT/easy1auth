plugins { id("easy1auth.java-library") }
dependencies {
 api(project(":modules:foundation")); implementation(project(":modules:persistence"))
 implementation(project(":modules:tenant"))
 implementation(project(":modules:security-policy"))
 annotationProcessor(libs.jimmer.apt)
}
