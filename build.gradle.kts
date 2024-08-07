// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        classpath(libs.bundles.classpath)

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle.kts files
    }
}

val sdkVersion: Int by extra(34)
val minimumSdkVersion: Int by extra(21)
val javaVersion: JavaVersion by extra(JavaVersion.VERSION_17)

val pitestMutators: Set<String> by extra(
    setOf(
        "STRONGER",
        "INLINE_CONSTS",
        "REMOVE_CONDITIONALS",
        "REMOVE_INCREMENTS",
        "EXPERIMENTAL_MEMBER_VARIABLE",
        "EXPERIMENTAL_NAKED_RECEIVER",
    )
)

plugins {
    base
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.dependency.analysis)
    alias(libs.plugins.task.tree)
}

tasks.detekt {
    jvmTarget = javaVersion.toString()
}

tasks.detektBaseline {
    jvmTarget = javaVersion.toString()
}

detekt {
    toolVersion = libs.versions.detekt.get()
    basePath = projectDir.toString()
    parallel = true
}
