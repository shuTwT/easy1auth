plugins { base }

group = "com.easy1auth"
version = "0.1.0-SNAPSHOT"

tasks.register("checkArchitecture") {
    group = "verification"
    dependsOn(gradle.includedBuild("build-logic").task(":check"))
}
