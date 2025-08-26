import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java-library")
    id("kotlin")
    alias(libs.plugins.ktfmt)
    id("io.gitlab.arturbosch.detekt")
    alias(libs.plugins.dependency.analysis)
}

val javaVersion: JavaVersion by rootProject.extra

java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(javaVersion.toString())
        javaParameters = true
    }
}

ktfmt { kotlinLangStyle() }

dependencies {
    api(libs.kotlin.stdlib)
    api(libs.koin.annotations)
    api(libs.ksp.api)
    api(libs.kotlinpoet)

    implementation(projects.ian.annotations)
    implementation(projects.ian.listener)
    implementation(projects.ian.util)
    implementation(libs.kotlinpoet.ksp)
}
