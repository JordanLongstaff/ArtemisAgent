pluginManagement { repositories { gradlePluginPortal() } }

plugins { id("dev.panuszewski.typesafe-conventions") version "0.8.0" }

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        gradlePluginPortal()
    }
}

rootProject.name = "build-logic"
