import com.android.build.gradle.internal.tasks.factory.dependsOn
import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.serialization")
    alias(libs.plugins.google.services)
    alias(libs.plugins.crashlytics)
    alias(libs.plugins.protobuf)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktfmt)
    alias(libs.plugins.dependency.analysis)
}

val appName = "Artemis Agent"
val appId = "artemis.agent"
val sdkVersion: Int by rootProject.extra
val minimumSdkVersion: Int by rootProject.extra
val javaVersion: JavaVersion by rootProject.extra
val stringRes = "string"

val release = "release"
val keystoreProperties =
    Properties().apply { load(FileInputStream(rootProject.file("keystore.properties"))) }

val kotlinMainPath: String by rootProject.extra
val kotlinTestPath: String by rootProject.extra
val kotlinAndroidTestPath = "src/androidTest/kotlin"

android {
    namespace = appId
    compileSdk = sdkVersion

    defaultConfig {
        applicationId = appId
        minSdk = minimumSdkVersion
        targetSdk = sdkVersion
        versionCode = 30
        versionName = "1.0.9"
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
    testOptions.unitTests.all { it.useJUnitPlatform() }

    signingConfigs {
        create(release) {
            keyAlias = keystoreProperties["keyAlias"] as String
            keyPassword = keystoreProperties["keyPassword"] as String
            storePassword = keystoreProperties["storePassword"] as String
            storeFile = file(keystoreProperties["storeFile"] as String)
        }
    }

    buildTypes {
        configureEach {
            resValue(stringRes, "app_name", appName)
            resValue(stringRes, "app_version", "$appName ${defaultConfig.versionName}")
        }
        release {
            signingConfig = signingConfigs.getByName(release)
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
    testImplementation(projects.ian.testing)

    ksp(projects.ian.processor)

    implementation(libs.bundles.app)
    debugImplementation(libs.bundles.app.debug)

    testImplementation(libs.bundles.app.test)
    testRuntimeOnly(libs.bundles.app.test.runtime)

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
    source.setFrom(files(kotlinMainPath, kotlinTestPath, kotlinAndroidTestPath))
    ignoredBuildTypes = listOf(release)
    ignoredVariants = listOf(release)
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
