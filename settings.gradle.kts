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
    "common", "modules:tenant",
    "modules:admin-identity", "modules:admin-access", "modules:pool-identity",
    "modules:application", "modules:security-policy",
    "modules:social-identity", "modules:enterprise-identity", "modules:customization", "modules:audit", "modules:oauth2-core",
    "apps:admin-api", "apps:authorization-server", "database-migration"
)
