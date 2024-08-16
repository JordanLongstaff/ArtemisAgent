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

val sdkVersion: Int by extra(34)
val minimumSdkVersion: Int by extra(21)
val javaVersion: JavaVersion by extra(JavaVersion.VERSION_17)

val pitestTimeoutFactor: BigDecimal by extra(BigDecimal(100))
val pitestMutators: Set<String> by extra(
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

plugins {
    base
    alias(libs.plugins.detekt)
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.dependency.analysis)
    alias(libs.plugins.task.tree)
}

tasks.detekt {
    jvmTarget = javaVersion.toString()

    reports {
        sarif {
            required = true
        }
    }
}

tasks.detektBaseline {
    jvmTarget = javaVersion.toString()
}

detekt {
    toolVersion = libs.versions.detekt.get()
    basePath = projectDir.toString()
    parallel = true
}
