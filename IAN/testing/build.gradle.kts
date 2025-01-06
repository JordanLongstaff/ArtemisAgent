import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java-library")
    id("kotlin")
    alias(libs.plugins.ktfmt)
    alias(libs.plugins.detekt)
    alias(libs.plugins.dependency.analysis)
}

val javaVersion: JavaVersion by rootProject.extra

java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

tasks.compileKotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(javaVersion.toString())
        javaParameters = true
    }
}

ktfmt { kotlinLangStyle() }

detekt {
    source.setFrom(file("src/main/kotlin"))
    config.setFrom(file("$rootDir/config/detekt/detekt.yml"))
}

dependencies { implementation(libs.kotest.framework.engine) }
