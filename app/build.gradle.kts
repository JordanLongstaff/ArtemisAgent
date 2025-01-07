import com.android.build.gradle.internal.tasks.factory.dependsOn

plugins {
    id("com.android.application")
    kotlin("android")
    alias(libs.plugins.google.services)
    alias(libs.plugins.crashlytics)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktfmt)
    alias(libs.plugins.dependency.analysis)
}

val appName: String = "Artemis Agent"
val sdkVersion: Int by rootProject.extra
val minimumSdkVersion: Int by rootProject.extra
val javaVersion: JavaVersion by rootProject.extra

android {
    namespace = "artemis.agent"
    compileSdk = sdkVersion

    defaultConfig {
        applicationId = "artemis.agent"
        minSdk = minimumSdkVersion
        targetSdk = sdkVersion
        versionCode = 18
        versionName = "1.0.8"
        multiDexEnabled = true

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
    }

    compileOptions {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions { jvmTarget = javaVersion.toString() }

    testOptions.execution = "ANDROIDX_TEST_ORCHESTRATOR"

    buildTypes {
        configureEach {
            resValue("string", "app_name", appName)
            resValue("string", "app_version", "$appName ${defaultConfig.versionName}")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )

            ndk.debugSymbolLevel = "FULL"
        }
    }

    applicationVariants.all {
        val variant = name.substring(0, 1).uppercase() + name.substring(1)
        tasks.named("assemble$variant").dependsOn(":app:konsist:test${variant}UnitTest")
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    tasks.preBuild.dependsOn(":IAN:konsistCollect")
}

dependencies {
    implementation(fileTree(baseDir = "libs") { include("*.jar") })
    implementation(projects.ian)
    implementation(projects.ian.enums)
    implementation(projects.ian.listener)
    implementation(projects.ian.packets)
    implementation(projects.ian.udp)
    implementation(projects.ian.util)
    implementation(projects.ian.vesseldata)
    implementation(projects.ian.world)

    ksp(projects.ian.processor)

    implementation(libs.bundles.app)
    debugImplementation(libs.bundles.app.debug)
    androidTestImplementation(libs.bundles.app.androidTest)
    androidTestUtil(libs.test.orchestrator)

    implementation(platform(libs.firebase.bom))
    implementation(libs.bundles.firebase)

    constraints {
        implementation(libs.guava) {
            because("Version 32.0.0-android patches a moderate security vulnerability")
        }
        androidTestImplementation(libs.jsoup) {
            because("Version 1.14.2 patches a high-level security vulnerability")
        }
        androidTestImplementation(libs.accessibility.test.framework) {
            because("Needed to resolve static method registerDefaultInstance")
        }
    }

    coreLibraryDesugaring(libs.desugaring)
}

ktfmt { kotlinLangStyle() }

detekt {
    source.setFrom(file("src/main/kotlin"))
    config.setFrom(file("$rootDir/config/detekt/detekt.yml"))
    ignoredBuildTypes = listOf("release")
    ignoredVariants = listOf("release")
}

protobuf {
    protoc { artifact = libs.protoc.get().toString() }

    generateProtoTasks {
        all().forEach {
            it.builtins {
                create("java") { option("lite") }
                create("kotlin") { option("lite") }
            }
        }
    }
}
