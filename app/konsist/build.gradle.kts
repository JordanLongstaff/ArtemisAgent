import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.android.library")
    kotlin("android")
    alias(libs.plugins.detekt)
    alias(libs.plugins.dependency.analysis)
}

val sdkVersion: Int by rootProject.extra
val minimumSdkVersion: Int by rootProject.extra
val javaVersion: JavaVersion by rootProject.extra

android {
    namespace = "artemis.agent"
    compileSdk = sdkVersion

    defaultConfig {
        minSdk = minimumSdkVersion
        multiDexEnabled = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    lint.sarifReport = true

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget = JvmTarget.fromTarget(javaVersion.toString())
            javaParameters = true
        }
    }

    tasks.withType<Test>().configureEach { useJUnitPlatform() }
}

dependencies {
    testImplementation(projects.app)

    testImplementation(platform(libs.kotest.bom))
    testImplementation(libs.bundles.konsist.app)
    testImplementation(libs.bundles.konsist.common)
    testRuntimeOnly(libs.bundles.konsist.runtime)
}

dependencyAnalysis { issues { ignoreSourceSet("androidTest") } }
