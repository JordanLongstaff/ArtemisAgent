import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java-gradle-plugin")
    `kotlin-dsl`
}

val javaVersion = JavaVersion.VERSION_17

java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

kotlin { compilerOptions { jvmTarget = JvmTarget.fromTarget(javaVersion.toString()) } }

dependencies { implementation(libs.bundles.classpath) }
