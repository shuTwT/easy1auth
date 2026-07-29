plugins { id("easy1auth.java-library") }
dependencies { api(project(":modules:foundation")); implementation(project(":modules:directory")); implementation(project(":modules:persistence")); annotationProcessor(libs.jimmer.apt); testImplementation(libs.spring.test) }
