plugins { id("easy1auth.java-library") }
dependencies { api(project(":modules:infrastructure")); annotationProcessor(libs.jimmer.apt) }
