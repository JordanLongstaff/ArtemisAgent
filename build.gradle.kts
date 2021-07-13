// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        classpath(libs.bundles.classpath)

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle.kts files
    }
}

val sdkVersion by extra(34)
val minimumSdkVersion by extra(21)
val javaVersion by extra(JavaVersion.VERSION_17)

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
