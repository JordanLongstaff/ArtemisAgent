// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    dependencies {
        classpath(libs.bundles.classpath)

        constraints {
            classpath(libs.commons.compress) {
                because("Version 1.26 patches two high-level security vulnerabilities")
            }
            classpath(libs.netty) {
                because("Version 4.1.100.Final patches a high-level security vulnerability")
            }
            classpath(libs.bouncycastle) {
                because("Version 1.78 patches three moderate security vulnerabilities")
            }
        }

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle.kts files
    }
}

val sdkVersion: Int by extra(36)
val minimumSdkVersion: Int by extra(21)
val javaVersion: JavaVersion by extra(JavaVersion.VERSION_21)

val pitestTimeoutFactor: BigDecimal by extra(BigDecimal(100))
val pitestMutators: Set<String> by
    extra(
        setOf(
            "STRONGER",
            "EXTENDED",
            "EXTREME",
            "INLINE_CONSTS",
            "REMOVE_CONDITIONALS",
            "REMOVE_INCREMENTS",
            "EXPERIMENTAL_MEMBER_VARIABLE",
            "EXPERIMENTAL_NAKED_RECEIVER",
        )
    )

val kotlinMainPath: String by extra("src/main/kotlin")
val kotlinTestPath: String by extra("src/test/kotlin")
val kotlinTestFixturesPath: String by extra("src/testFixtures/kotlin")

plugins {
    base
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.crashlytics) apply false
    alias(libs.plugins.dependency.analysis)
    alias(libs.plugins.task.tree)
    alias(libs.plugins.git.hooks)
}

tasks.detekt { jvmTarget = javaVersion.toString() }

tasks.detektBaseline { jvmTarget = javaVersion.toString() }

dependencyAnalysis {
    usage {
        analysis {
            checkSuperClasses(true)
        }
    }
    useTypesafeProjectAccessors(true)
}

detekt {
    toolVersion = libs.versions.detekt.get()
    basePath = projectDir.toString()
    parallel = true
}

gitHooks { setHooks(mapOf("pre-push" to "detekt ktfmtCheck")) }
