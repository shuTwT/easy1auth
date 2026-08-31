plugins { id("easy1auth.java-library") }
dependencies {
    api(project(":common"))
 implementation(project(":modules:connection"))
 annotationProcessor(libs.jimmer.apt)
}
