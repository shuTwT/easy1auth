plugins { id("easy1auth.java-library") }

dependencies {
    api(libs.spring.web)
    api(libs.spring.aop)
    implementation(libs.redisson.spring.boot.starter)
    api(libs.jimmer.spring)
    implementation(libs.hutool.all)
    annotationProcessor(libs.jimmer.apt)
    runtimeOnly(libs.postgresql)
    testImplementation(libs.spring.test)
}
