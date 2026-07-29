pluginManagement {
    includeBuild("build-logic")
    repositories { gradlePluginPortal(); mavenCentral() }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { mavenCentral() }
}

rootProject.name = "easy1auth-backend"

include(
    "modules:foundation", "modules:persistence", "modules:tenant",
    "modules:admin-identity", "modules:admin-access", "modules:directory",
    "modules:user-access", "modules:application", "modules:security-policy",
    "modules:federation", "modules:customization", "modules:audit", "modules:oauth2-core",
    "apps:admin-api", "apps:authorization-server", "database-migration"
)
