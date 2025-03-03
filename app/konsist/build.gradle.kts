plugins {
    id("com.android.library")
    kotlin("android")
    alias(libs.plugins.detekt)
    alias(libs.plugins.ktfmt)
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

        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    kotlinOptions { jvmTarget = javaVersion.toString() }

    tasks.withType<Test>().configureEach { useJUnitPlatform() }
}

dependencies {
    testImplementation(projects.app)
    testImplementation(projects.ian.testing)

    testImplementation(libs.bundles.konsist.app)
    testImplementation(libs.bundles.konsist.common)
    testRuntimeOnly(libs.bundles.konsist.runtime)
}

ktfmt { kotlinLangStyle() }

dependencyAnalysis { issues { ignoreSourceSet("androidTest") } }
