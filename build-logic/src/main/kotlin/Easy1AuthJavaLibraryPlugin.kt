import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.springframework.boot.gradle.plugin.SpringBootPlugin

class Easy1AuthJavaLibraryPlugin : Plugin<Project> {
    override fun apply(project: Project) = with(project) {
        pluginManager.apply("java-library")
        pluginManager.apply("io.spring.dependency-management")
        extensions.configure(DependencyManagementExtension::class.java) {
            imports { mavenBom(SpringBootPlugin.BOM_COORDINATES) }
        }
        dependencies.add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
        extensions.configure(JavaPluginExtension::class.java) {
            toolchain.languageVersion.set(JavaLanguageVersion.of(21))
            withSourcesJar()
        }
        tasks.withType(Test::class.java).configureEach { useJUnitPlatform() }
    }
}
