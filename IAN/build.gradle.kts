import com.android.build.gradle.internal.tasks.factory.dependsOn
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("java-library")
    id("kotlin")
    jacoco
    id("info.solidsoft.pitest")
    alias(libs.plugins.detekt)
}

val javaVersion: JavaVersion by rootProject.extra

java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

tasks.compileKotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(javaVersion.toString())
        javaParameters = true
    }
}

tasks.test {
    useJUnitPlatform()

    configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

tasks.assemble.dependsOn(":IAN:konsist:test")
tasks.pitest.dependsOn(tasks.jacocoTestReport)

detekt {
    source.setFrom(file("src/main/kotlin"))
    config.setFrom(file("config/detekt/detekt.yml"))
}

dependencies {
    api(libs.kotlin.stdlib)
    implementation(libs.bundles.ian)
    testImplementation(libs.bundles.ian.test)
    testRuntimeOnly(libs.bundles.ian.test.runtime)
}

jacoco {
    toolVersion = libs.versions.jacoco.get()
}

pitest {
    targetClasses.set(listOf("com.walkertribe.ian.*"))
    threads.set(2)
    outputFormats.set(listOf("HTML", "CSV"))
    timestampedReports.set(false)
}
