plugins {
    id("com.android.library")
    kotlin("android")
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
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    kotlinOptions {
        jvmTarget = javaVersion.toString()
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    tasks.whenTaskAdded {
        enabled = !name.contains("AndroidTest")
    }
}

dependencies {
    testImplementation(project(":app"))

    testImplementation(libs.bundles.konsist.app)
    testImplementation(libs.bundles.konsist.common)
    testRuntimeOnly(libs.bundles.konsist.runtime)
}

dependencyAnalysis {
    issues {
        ignoreSourceSet("androidTest")
    }
}
