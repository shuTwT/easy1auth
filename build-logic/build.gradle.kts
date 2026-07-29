plugins { `kotlin-dsl` }

dependencies {
    implementation("org.springframework.boot:spring-boot-gradle-plugin:3.5.16")
    implementation("io.spring.gradle:dependency-management-plugin:1.1.7")
}

gradlePlugin {
    plugins {
        register("javaLibrary") {
            id = "easy1auth.java-library"
            implementationClass = "Easy1AuthJavaLibraryPlugin"
        }
        register("springApplication") {
            id = "easy1auth.spring-application"
            implementationClass = "Easy1AuthSpringApplicationPlugin"
        }
    }
}
