import artemis.agent.gradle.configure
import artemis.agent.gradle.dependsOnKonsist
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java-library")
    id("kotlin")
    id("org.jetbrains.kotlinx.kover")
    id("info.solidsoft.pitest")
    id("io.gitlab.arturbosch.detekt")
    alias(libs.plugins.dependency.analysis)
}

val javaVersion: JavaVersion by rootProject.extra

java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(javaVersion.toString())
        javaParameters = true
    }
}

tasks.test {
    jvmArgs("-Xmx2g", "-Xms1g", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:+UseParallelGC")
    useJUnitPlatform()
}

dependsOnKonsist()

dependencies {
    implementation(libs.kotlinx.io)
    api(libs.bundles.ian.udp.api)

    testImplementation(libs.bundles.ian.udp.test)
    testRuntimeOnly(libs.bundles.ian.test.runtime)

    pitest(libs.bundles.arcmutate)
}

pitest.configure(rootPackage = "com.walkertribe.ian.protocol", threads = 2)
