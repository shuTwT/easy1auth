plugins {
    id("easy1auth.java-library")
}

dependencies {
    api(libs.spring.data.redis)
    implementation(libs.redisson.spring.boot.starter)
    implementation(libs.hutool.all)
}
