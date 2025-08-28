import artemis.agent.gradle.configure
import artemis.agent.gradle.dependsOnKonsist
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java-library")
    id("kotlin")
    fixtures
    alias(libs.plugins.ksp)
    id("info.solidsoft.pitest")
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
    api(projects.ian.enums)
    api(projects.ian.listener)
    api(projects.ian.util)
    api(projects.ian.vesseldata)
    api(libs.kotlin.stdlib)

    ksp(projects.ian.processor)

    testFixturesApi(projects.ian.listener)
    testFixturesApi(projects.ian.util)
    testFixturesImplementation(libs.bundles.ian.world.test.fixtures)

    testImplementation(testFixtures(projects.ian.util))
    testImplementation(testFixtures(projects.ian.vesseldata))
    testImplementation(libs.bundles.ian.world.test)
    testRuntimeOnly(libs.bundles.ian.test.runtime)

    pitest(libs.bundles.arcmutate)
}

pitest.configure(rootPackage = "com.walkertribe.ian.world", threads = 2)
