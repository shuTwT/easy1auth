plugins { id("easy1auth.java-library") }
dependencies {
    api(project(":common"))
 implementation(project(":modules:social-identity"))
 annotationProcessor(libs.jimmer.apt)
}
