import artemis.agent.gradle.configure
import artemis.agent.gradle.configureTests
import artemis.agent.gradle.dependsOnKonsist

plugins {
    id("ian-library")
    id("fixtures")
    alias(libs.plugins.ksp)
    id("info.solidsoft.pitest")
}

configureTests(maxMemoryGb = 4)

pitest {
    configure(rootPackage = "com.walkertribe.ian.protocol", threads = 8)
    jvmArgs = listOf("-Xmx8g", "-Xms1g", "-XX:+HeapDumpOnOutOfMemoryError", "-XX:+UseParallelGC")
}

dependsOnKonsist()

dependencies {
    compileOnly(projects.ian.annotations)

    api(projects.ian.enums)
    api(projects.ian.listener)
    api(projects.ian.util)
    api(projects.ian.vesseldata)
    api(projects.ian.world)

    api(libs.bundles.ian.packets.api)

    ksp(projects.ian.processor)
    ksp(libs.ksp.koin)

    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.io)

    testImplementation(testFixtures(projects.ian.listener))
    testImplementation(testFixtures(projects.ian.vesseldata))
    testImplementation(testFixtures(projects.ian.world))

    testFixturesApi(projects.ian.enums)
    testFixturesApi(projects.ian.listener)
    testFixturesApi(projects.ian.util)
    testFixturesApi(projects.ian.vesseldata)
    testFixturesApi(projects.ian.world)

    testFixturesApi(libs.kotlinx.io)

    testFixturesImplementation(testFixtures(projects.ian.enums))
    testFixturesImplementation(testFixtures(projects.ian.listener))
    testFixturesImplementation(testFixtures(projects.ian.util))
    testFixturesImplementation(testFixtures(projects.ian.vesseldata))
    testFixturesImplementation(testFixtures(projects.ian.world))

    testFixturesImplementation(libs.bundles.ian.packets.test.fixtures)
    testImplementation(libs.bundles.ian.packets.test)
    testRuntimeOnly(libs.bundles.ian.test.runtime)

    pitest(libs.bundles.arcmutate)
}
