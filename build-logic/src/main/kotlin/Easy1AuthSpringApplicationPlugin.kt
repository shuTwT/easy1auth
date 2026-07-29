import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion

class Easy1AuthSpringApplicationPlugin : Plugin<Project> {
    override fun apply(project: Project) = with(project) {
        pluginManager.apply("java")
        pluginManager.apply("org.springframework.boot")
        pluginManager.apply("io.spring.dependency-management")
        dependencies.add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
        extensions.configure(JavaPluginExtension::class.java) {
            toolchain.languageVersion.set(JavaLanguageVersion.of(21))
        }
        tasks.withType(Test::class.java).configureEach { useJUnitPlatform() }
    }
}
