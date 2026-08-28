plugins { id("easy1auth.java-library") }
dependencies {
 api(project(":modules:infrastructure"))
 implementation(project(":modules:social-identity"))
 annotationProcessor(libs.jimmer.apt)
}
