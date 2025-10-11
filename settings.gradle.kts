pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        maven { url = uri("https://jitpack.io") }
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "ArtemisAgent"

includeBuild("build-logic")
include(
    ":app",
    ":app:konsist",
    ":IAN",
    ":IAN:annotations",
    ":IAN:annotations:konsist",
    ":IAN:enums",
    ":IAN:enums:konsist",
    ":IAN:listener",
    ":IAN:listener:konsist",
    ":IAN:packets",
    ":IAN:packets:konsist",
    ":IAN:processor",
    ":IAN:udp",
    ":IAN:udp:konsist",
    ":IAN:util",
    ":IAN:vesseldata",
    ":IAN:vesseldata:konsist",
    ":IAN:world",
    ":IAN:world:konsist",
)
