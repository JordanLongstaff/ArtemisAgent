import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java-gradle-plugin")
    `kotlin-dsl`
    alias(libs.plugins.ktfmt)
    alias(libs.plugins.detekt)
    alias(libs.plugins.dependency.analysis)
}

val javaVersion = JavaVersion.VERSION_17

java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

kotlin { compilerOptions { jvmTarget = JvmTarget.fromTarget(javaVersion.toString()) } }

ktfmt { kotlinLangStyle() }

detekt {
    toolVersion = libs.versions.detekt.get()
    basePath = projectDir.toString()
    parallel = true
}

dependencies { implementation(libs.bundles.classpath) }
