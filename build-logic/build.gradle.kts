import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `java-gradle-plugin`
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

dependencies {
    implementation(libs.bundles.build.logic)
    api(libs.bundles.build.logic.api)
    runtimeOnly(libs.gradle)

    constraints {
        runtimeOnly(libs.commons.compress) {
            because("Version 1.26 patches two high-level security vulnerabilities")
        }
        runtimeOnly(libs.netty) {
            because("Version 4.1.100.Final patches a high-level security vulnerability")
        }
    }
}
