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
    "framework:framework-common", "framework:framework-web",
    "framework:framework-persistence", "framework:framework-tenant",
    "framework:framework-mq", "modules:tenant",
    "modules:system", "modules:pool-identity",
    "modules:application", "modules:security-policy",
    "modules:connection", "modules:customization", "modules:audit", "modules:oauth2-core",
    "apps:admin-api", "apps:authorization-server", "database-migration"
)
